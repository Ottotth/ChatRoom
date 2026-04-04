#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$ROOT_DIR/chatroom-frontend"
BACKEND_DIR="$ROOT_DIR/chatroom"
BUILD_DIR="$ROOT_DIR/.deploy-build"

if [[ -f "$ROOT_DIR/.deploy.env" ]]; then
  # shellcheck disable=SC1091
  source "$ROOT_DIR/.deploy.env"
fi

INSTALL_LOCAL_POSTGRES="${INSTALL_LOCAL_POSTGRES:-true}"
POSTGRES_DB="${POSTGRES_DB:-chatroom_db}"
POSTGRES_USER="${POSTGRES_USER:-chatroom_user}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-}"

required_vars=(
  EC2_HOST
  EC2_USER
  EC2_KEY_PATH
  DOMAIN_NAME
)

for var in "${required_vars[@]}"; do
  if [[ -z "${!var:-}" ]]; then
    echo "[ERROR] Missing required variable: $var"
    echo "        Set it in environment or in $ROOT_DIR/.deploy.env"
    exit 1
  fi
done

if [[ "$INSTALL_LOCAL_POSTGRES" == "true" ]]; then
  if [[ -z "$POSTGRES_PASSWORD" ]]; then
    echo "[ERROR] Missing required variable: POSTGRES_PASSWORD"
    echo "        Set it in environment or in $ROOT_DIR/.deploy.env"
    exit 1
  fi
  DB_URL="${DB_URL:-jdbc:postgresql://127.0.0.1:5432/${POSTGRES_DB}}"
  DB_USERNAME="${DB_USERNAME:-$POSTGRES_USER}"
  DB_PASSWORD="${DB_PASSWORD:-$POSTGRES_PASSWORD}"
else
  db_required_vars=(DB_URL DB_USERNAME DB_PASSWORD)
  for var in "${db_required_vars[@]}"; do
    if [[ -z "${!var:-}" ]]; then
      echo "[ERROR] Missing required variable: $var"
      echo "        Set it in environment or in $ROOT_DIR/.deploy.env"
      exit 1
    fi
  done
fi

EC2_PORT="${EC2_PORT:-22}"
APP_PORT="${APP_PORT:-8080}"
JAVA_PACKAGE="${JAVA_PACKAGE:-openjdk-21-jre-headless}"
SERVICE_NAME="${SERVICE_NAME:-chatroom}"
REMOTE_TMP_DIR="${REMOTE_TMP_DIR:-/tmp/chatroom-deploy}"
REMOTE_APP_DIR="${REMOTE_APP_DIR:-/opt/chatroom}"
REMOTE_WEB_DIR="${REMOTE_WEB_DIR:-/var/www/chatroom}"
BACKEND_JAR_PATH="${BACKEND_JAR_PATH:-}"
CERTBOT_EMAIL="${CERTBOT_EMAIL:-}"

REMOTE_INSTALL_PACKAGES="nginx ${JAVA_PACKAGE}"
if [[ "$INSTALL_LOCAL_POSTGRES" == "true" ]]; then
  REMOTE_INSTALL_PACKAGES="${REMOTE_INSTALL_PACKAGES} postgresql postgresql-contrib"
fi
if [[ -n "$CERTBOT_EMAIL" ]]; then
  REMOTE_INSTALL_PACKAGES="${REMOTE_INSTALL_PACKAGES} certbot python3-certbot-nginx"
fi

SSH_OPTS=(-i "$EC2_KEY_PATH" -p "$EC2_PORT" -o StrictHostKeyChecking=accept-new)
SCP_OPTS=(-i "$EC2_KEY_PATH" -P "$EC2_PORT" -o StrictHostKeyChecking=accept-new)

echo "[1/7] Building frontend"
pushd "$FRONTEND_DIR" >/dev/null
npm ci
npm run build
popd >/dev/null

echo "[2/7] Building backend"
pushd "$BACKEND_DIR" >/dev/null
./mvnw -DskipTests clean package
popd >/dev/null

if [[ -z "$BACKEND_JAR_PATH" ]]; then
  BACKEND_JAR_PATH="$(find "$BACKEND_DIR/target" -maxdepth 1 -type f -name '*.jar' ! -name '*.jar.original' | head -n 1)"
fi

if [[ -z "$BACKEND_JAR_PATH" || ! -f "$BACKEND_JAR_PATH" ]]; then
  echo "[ERROR] Backend jar not found. Set BACKEND_JAR_PATH in .deploy.env"
  exit 1
fi

echo "[3/7] Preparing artifacts"
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
cp "$BACKEND_JAR_PATH" "$BUILD_DIR/app.jar"
tar -C "$FRONTEND_DIR/dist" -czf "$BUILD_DIR/frontend.tar.gz" .

echo "[4/7] Uploading artifacts to EC2"
ssh "${SSH_OPTS[@]}" "$EC2_USER@$EC2_HOST" "mkdir -p '$REMOTE_TMP_DIR'"
scp "${SCP_OPTS[@]}" "$BUILD_DIR/app.jar" "$BUILD_DIR/frontend.tar.gz" "$EC2_USER@$EC2_HOST:$REMOTE_TMP_DIR/"

echo "[5/7] Installing runtime and deploying services on EC2"
D='$'
ssh "${SSH_OPTS[@]}" "$EC2_USER@$EC2_HOST" bash -s <<EOF
set -euo pipefail

if command -v apt-get >/dev/null 2>&1; then
  sudo apt-get update
  sudo apt-get install -y ${REMOTE_INSTALL_PACKAGES}
else
  echo "This script currently supports Ubuntu/Debian EC2 (apt-get)."
  exit 1
fi

if [[ "${INSTALL_LOCAL_POSTGRES}" == "true" ]]; then
  echo "Configuring local PostgreSQL..."
  sudo systemctl enable postgresql
  sudo systemctl restart postgresql

  if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_roles WHERE rolname='${POSTGRES_USER}'" | grep -q 1; then
    sudo -u postgres psql -c "CREATE ROLE \"${POSTGRES_USER}\" LOGIN PASSWORD '${POSTGRES_PASSWORD}'"
  else
    sudo -u postgres psql -c "ALTER ROLE \"${POSTGRES_USER}\" WITH LOGIN PASSWORD '${POSTGRES_PASSWORD}'"
  fi

  if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='${POSTGRES_DB}'" | grep -q 1; then
    sudo -u postgres psql -c "CREATE DATABASE \"${POSTGRES_DB}\" OWNER \"${POSTGRES_USER}\""
  fi
fi

sudo mkdir -p "${REMOTE_APP_DIR}" "${REMOTE_WEB_DIR}" "${REMOTE_APP_DIR}/uploads/avatars"
sudo mv "${REMOTE_TMP_DIR}/app.jar" "${REMOTE_APP_DIR}/app.jar"
sudo tar -xzf "${REMOTE_TMP_DIR}/frontend.tar.gz" -C "${REMOTE_WEB_DIR}"
sudo rm -f "${REMOTE_TMP_DIR}/frontend.tar.gz"

sudo tee "/etc/systemd/system/${SERVICE_NAME}.service" >/dev/null <<SERVICE
[Unit]
Description=ChatRoom Spring Boot Service
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=${REMOTE_APP_DIR}
Environment=DB_URL=${DB_URL}
Environment=DB_USERNAME=${DB_USERNAME}
Environment=DB_PASSWORD=${DB_PASSWORD}
Environment=SERVER_PORT=${APP_PORT}
ExecStart=/usr/bin/java -jar ${REMOTE_APP_DIR}/app.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
SERVICE

sudo tee /etc/nginx/sites-available/chatroom >/dev/null <<'NGINX'
server {
    listen 80;
    server_name ${DOMAIN_NAME};

    root ${REMOTE_WEB_DIR};
    index index.html;

    location / {
      try_files ${D}uri /index.html;
    }

    location /user {
        proxy_pass http://127.0.0.1:${APP_PORT};
        proxy_set_header Host ${D}host;
        proxy_set_header X-Real-IP ${D}remote_addr;
        proxy_set_header X-Forwarded-For ${D}proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto ${D}scheme;
    }

    location /rooms {
        proxy_pass http://127.0.0.1:${APP_PORT};
        proxy_set_header Host ${D}host;
        proxy_set_header X-Real-IP ${D}remote_addr;
        proxy_set_header X-Forwarded-For ${D}proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto ${D}scheme;
    }

    location /messages {
        proxy_pass http://127.0.0.1:${APP_PORT};
        proxy_set_header Host ${D}host;
        proxy_set_header X-Real-IP ${D}remote_addr;
        proxy_set_header X-Forwarded-For ${D}proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto ${D}scheme;
    }

    location /avatars {
        proxy_pass http://127.0.0.1:${APP_PORT};
        proxy_set_header Host ${D}host;
        proxy_set_header X-Real-IP ${D}remote_addr;
        proxy_set_header X-Forwarded-For ${D}proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto ${D}scheme;
    }

    location /ws {
        proxy_pass http://127.0.0.1:${APP_PORT};
        proxy_http_version 1.1;
        proxy_set_header Upgrade ${D}http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host ${D}host;
        proxy_set_header X-Real-IP ${D}remote_addr;
        proxy_set_header X-Forwarded-For ${D}proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto ${D}scheme;
    }
}
NGINX

sudo ln -sf /etc/nginx/sites-available/chatroom /etc/nginx/sites-enabled/chatroom
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t

sudo systemctl daemon-reload
sudo systemctl enable ${SERVICE_NAME}
sudo systemctl restart ${SERVICE_NAME}
sudo systemctl enable nginx
sudo systemctl restart nginx

if [[ -n "${CERTBOT_EMAIL}" ]]; then
  DOMAIN_CERT="/etc/letsencrypt/live/${DOMAIN_NAME}/fullchain.pem"
  if sudo test -f "${D}DOMAIN_CERT"; then
    echo "SSL certificate found, reinstalling into nginx..."
    sudo certbot install --nginx -d "${DOMAIN_NAME}" --non-interactive || echo "Warning: certbot install failed"
  else
    echo "Obtaining SSL certificate from Let's Encrypt..."
    sudo certbot --nginx -d "${DOMAIN_NAME}" --non-interactive --agree-tos --email "${CERTBOT_EMAIL}" --redirect || echo "Warning: certbot failed, site will use HTTP"
  fi
  sudo systemctl reload nginx || true
fi

echo "Deployment completed successfully."
echo "Service status:"
sudo systemctl --no-pager --full status ${SERVICE_NAME} | head -n 20 || true
EOF

echo "[6/7] Cleaning local temporary files"
rm -rf "$BUILD_DIR"

echo "[7/7] Done"
if [[ -n "$CERTBOT_EMAIL" ]]; then
  echo "Open: https://$DOMAIN_NAME"
else
  echo "Open: http://$DOMAIN_NAME"
  echo "Tip: Add CERTBOT_EMAIL=your@email.com to .deploy.env to enable HTTPS automatically."
fi
