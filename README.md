# 🔍 Wide Events Demo — Observabilidade Moderna para Checkout

> **"Pare de logar o que seu código faz. Logue o que aconteceu com a request."**

Projeto de demonstração que implementa o padrão **Wide Events** (também chamado de Canonical Log Lines) em um serviço de checkout simulado, com stack de observabilidade completa usando **Grafana + Loki**.

Inspirado em [Logging Sucks](https://loggingsucks.com/) — Boris Tane.

---

## 🧠 O que são Wide Events?

Em vez de 15 `log.info()` espalhados pelo código, emitimos **um único evento estruturado e rico** por request, no final do processamento:

```json
{
  "request_id": "a1b2c3d4",
  "service": "checkout-service",
  "duration_ms": 847,
  "user_id": "user_4521",
  "user_subscription": "premium",
  "user_account_age_days": 847,
  "action": "checkout",
  "cart_total_cents": 15999,
  "payment_provider": "stripe",
  "payment_latency_ms": 423,
  "payment_attempt": 2,
  "outcome": "error",
  "error_code": "card_declined",
  "feature_flags": { "new_checkout_flow": true }
}
```

**Isso permite queries impossíveis com logs tradicionais:**

- *"Falhas de checkout de usuários premium na última hora"*
- *"Taxa de erro por payment provider nos últimos 30min"*
- *"Usuários enterprise com mais de 2 tentativas de pagamento"*

## 🏗️ Arquitetura

```
┌─────────────────┐     stdout     push      ┌──────┐
│ Checkout Service │ ──── JSON  ───────────▶ │ Loki │
│  (Spring Boot)   │                         └──┬───┘
└─────────────────┘                             │
        │                                       │
        │ /checkout                  ┌──────────┴──────────┐
        │                            │      Grafana        │
  ┌─────┴──────┐                     │  - Dashboards       │
  │ Simulador  │                     │  - LogQL queries    │
  │ de Tráfego │                     │  - Alertas          │
  └────────────┘                     └─────────────────────┘
```

## 🚀 Como rodar

### Pré-requisitos
- Docker e Docker Compose

### Subir tudo

```bash
docker compose up --build -d
```

### Acessar

| Serviço | URL                                                |
|---------|----------------------------------------------------|
| **Grafana** (dashboards) | http://localhost:3000 (user:admin, password:admin) |
| **API Checkout** | http://localhost:8080/checkout                     |
| **Loki** | http://localhost:3100                              |

O simulador de tráfego já inicia automaticamente gerando ~2 requests/segundo.

### Testar manualmente

```bash
# Checkout simples
curl -X POST http://localhost:8080/checkout

# Checkout com user específico
curl -X POST http://localhost:8080/checkout -H "X-User-Id: user_premium_42"
```

## 📊 Dashboards disponíveis

O Grafana já vem provisionado com o dashboard **"Wide Events — Checkout Observability"** contendo:

- **Overview**: total de requests, success rate, erros, erros de premium users
- **Trends**: checkout outcomes over time, error rate por subscription tier
- **Payment Analysis**: distribuição de error codes, falhas por provider
- **Wide Event Explorer**: logs raw + tabela detalhada de falhas

### Queries LogQL de exemplo

```logql
# Todos os wide events de erro
{app="checkout-service"} | json | mdc_outcome = `error`

# Falhas de premium users com cart > R$500
{app="checkout-service"} | json
  | mdc_outcome = `error`
  | mdc_user_subscription = `premium`
  | mdc_cart_total_cents > 50000

# Erros do provider Cielo na última hora
{app="checkout-service"} | json
  | mdc_outcome = `error`
  | mdc_payment_provider = `cielo`

# Latência do pagamento por provider
avg_over_time(
  {app="checkout-service"}
  | json
  | unwrap mdc_payment_latency_ms
  [5m]
) by (mdc_payment_provider)
```

## 🧩 Estrutura do projeto

```
wide-events-demo/
├── src/main/java/com/demo/wideevents/
│   ├── domain/
│   │   ├── WideEvent.java            # O evento rico (50+ campos)
│   │   └── WideEventContext.java      # ThreadLocal para construir o evento
│   ├── filter/
│   │   └── WideEventFilter.java       # Intercepta request, emite wide event no final
│   ├── service/
│   │   └── CheckoutService.java       # Lógica de negócio que enriquece o evento
│   ├── controller/
│   │   └── CheckoutController.java    # REST endpoint
│   └── simulator/
│       └── TrafficSimulator.java      # Gera tráfego automático
├── docker/
│   ├── grafana/
│       ├── provisioning/              # Datasources e dashboards auto-configurados
│       └── dashboards/                # Dashboard JSON pré-pronto
├── docker-compose.yml                 # Stack completa
├── Dockerfile
└── pom.xml
```

## 🔑 Conceitos demonstrados

| Conceito | Implementação |
|----------|--------------|
| **Wide Event / Canonical Log Line** | `WideEvent.java` + `WideEventFilter.java` |
| **Alta Cardinalidade** | `user_id`, `cart_id`, `request_id` como campos |
| **Alta Dimensionalidade** | 30+ campos por evento |
| **Request-scoped context** | `WideEventContext` com ThreadLocal |
| **Build-and-emit pattern** | Construído ao longo da request, logado uma vez no final |
| **Structured logging** | JSON via logstash-logback-encoder |
| **Log-based dashboards** | Grafana + Loki LogQL |

## 📝 Wide Event vs Log Estruturado vs Tracing

| | Log Estruturado | Wide Event | Distributed Tracing |
|-|----------------|------------|-------------------|
| Formato | JSON | JSON | Spans |
| Linhas por request | Muitas | **Uma** | Uma por serviço |
| Contexto de negócio | Parcial | **Completo** | Mínimo |
| Query por user_id | Difícil | **Trivial** | Possível |
| Dashboards de negócio | Não | **Sim** | Não |

## 🆓 Deploy gratuito (opções)

- **Local**: Docker Compose (este projeto)
- **Grafana Cloud**: Free tier com 50GB logs/mês
- **App Java**: Render, Railway ou Oracle Cloud Free Tier
- **Portfolio**: grave um vídeo/GIF dos dashboards funcionando

## Imagens do dashboard no grafana
![Dashboard de graficos](/img/dashboard-img.png)
![Dashboard dos logs](/img/dashboard-logs.png)


## 📚 Referências

- [Logging Sucks — Boris Tane](https://loggingsucks.com/)
- [Canonical Log Lines — Stripe](https://stripe.com/blog/canonical-log-lines)
- [Observability Engineering — Charity Majors](https://www.oreilly.com/library/view/observability-engineering/9781492076438/)
