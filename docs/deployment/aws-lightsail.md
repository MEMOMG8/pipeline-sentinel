# AWS Lightsail Single-Instance Deployment

This guide prepares Pipeline Sentinel for a small AWS Lightsail Ubuntu instance running Docker Compose behind the custom domain `pipeline.manuelperezgil.com`. It does not create AWS resources, require AWS credentials, or automate deployment.

Target architecture:

```text
Internet
-> Caddy on ports 80 and 443
-> Next.js frontend
-> Spring Boot backend at /api/*
-> PostgreSQL private inside the Docker network
```

The browser calls the backend through the same public origin, for example `https://pipeline.manuelperezgil.com/api/v1/health`. Caddy manages HTTPS certificates and redirects HTTP to HTTPS. The backend and PostgreSQL containers do not publish public host ports.

## Cost Guardrail

Before creating anything in AWS:

1. Create an AWS Budget alert for `$10` monthly spend.
2. Configure alerts at `80%` and `100%`.
3. Choose a Lightsail plan only if the AWS console shows that the selected plan is within your monthly limit in your selected region.
4. Stop if the plan, static IP, data transfer, or region pricing would exceed your budget.

This project is intended for a student portfolio budget of `$10/month` or less.

## Instance Setup

Create one Lightsail instance:

- Platform: Linux/Unix
- Blueprint: Ubuntu LTS
- Plan: only a plan that fits your budget in the selected region
- Networking: attach a static IP

DNS prerequisite:

- In Route 53, `pipeline.manuelperezgil.com` must have an `A` record pointing to the Lightsail static IP.
- Confirm DNS resolves before expecting Caddy to issue a certificate.

Configure the Lightsail firewall:

- Allow SSH `22` only from your current public IP.
- Allow HTTP `80` from the internet.
- Allow HTTPS `443` from the internet.
- Do not open PostgreSQL `5432`.
- Do not open backend `8080` or `8081`.
- Do not open frontend `3000`.

Caddy needs ports `80` and `443` open so it can complete automatic certificate management and serve HTTPS for `pipeline.manuelperezgil.com`.

## Server Setup

SSH into the instance.

Install Git:

```bash
sudo apt-get update
sudo apt-get install -y git ca-certificates curl
```

Install Docker Engine and the Docker Compose plugin using Docker's official Ubuntu instructions:

```bash
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker "$USER"
```

Log out and back in so the `docker` group membership applies, then verify:

```bash
docker version
docker compose version
```

Clone the repository:

```bash
git clone https://github.com/MEMOMG8/pipeline-sentinel.git
cd pipeline-sentinel
```

Create the AWS environment file:

```bash
cp .env.aws.example .env.aws
```

Generate a strong PostgreSQL password on the server:

```bash
openssl rand -base64 32
```

Edit `.env.aws` and replace `POSTGRES_PASSWORD` with that generated value:

```bash
nano .env.aws
```

Start the stack:

```bash
docker compose --env-file .env.aws -f compose.aws.yaml up --build -d
```

## Verification

Check container state:

```bash
docker compose --env-file .env.aws -f compose.aws.yaml ps
```

Check the backend through Caddy:

```bash
curl https://pipeline.manuelperezgil.com/api/v1/health
```

Expected health response:

```json
{"service":"pipeline-sentinel","status":"UP"}
```

Open the live dashboard in a browser:

```text
https://pipeline.manuelperezgil.com
```

Confirm HTTP redirects to HTTPS:

```bash
curl -I http://pipeline.manuelperezgil.com
```

Expected result: an HTTP redirect to `https://pipeline.manuelperezgil.com`.

Demo flow:

1. Upload `examples/transaction-events-rejected-demo.csv`.
2. Open the created run detail page.
3. Confirm the outcome is `REJECTED`.
4. Confirm there are five validation issues.
5. Confirm there is one quarantined record.

## Operations

View logs:

```bash
docker compose --env-file .env.aws -f compose.aws.yaml logs -f
```

View one service:

```bash
docker compose --env-file .env.aws -f compose.aws.yaml logs -f backend
docker compose --env-file .env.aws -f compose.aws.yaml logs -f frontend
docker compose --env-file .env.aws -f compose.aws.yaml logs -f caddy
docker compose --env-file .env.aws -f compose.aws.yaml logs -f postgres
```

Pull code and rebuild:

```bash
git pull
docker compose --env-file .env.aws -f compose.aws.yaml up --build -d
```

Restart services:

```bash
docker compose --env-file .env.aws -f compose.aws.yaml restart
```

Safe stop, preserving the PostgreSQL volume:

```bash
docker compose --env-file .env.aws -f compose.aws.yaml down
```

Dangerous reset, deleting PostgreSQL data:

```bash
docker compose --env-file .env.aws -f compose.aws.yaml down -v
```

Warning: `down -v` deletes the Docker volume that contains the local PostgreSQL database for this deployment.

## Limitations

- Single-instance deployment only.
- No high availability.
- No automated continuous deployment.
- HTTPS depends on the Route 53 `A` record resolving to the Lightsail static IP and ports `80` and `443` being reachable.
- PostgreSQL runs in a local Docker volume on the instance.
- Caddy certificates and configuration are stored in Docker named volumes.
- Backups are the operator's responsibility.
- No RDS, ECS, EKS, App Runner, Lambda, API Gateway, Terraform, queues, or cloud storage are included.
