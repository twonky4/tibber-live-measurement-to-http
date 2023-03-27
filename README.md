# tibber-live-measurement-to-http
Converter of the tibber API live measurement websocket endpoint to a http endpoint.

Start the service and call http://admin:pass@localhost:8080/api/v1/live-measurement

Service configuration:

```yaml
app:
  login: <--- basic login data
    user-name: admin
    password: pass
  tibber:
    websocket-url: wss://websocket-api.tibber.com/v1-beta/gql/subscriptions
    url: https://api.tibber.com/v1-beta/gql
    token: 5K4MVS-OjfWhK_4yrjOlFe1F6kJXPVf7eQYggo8ebAE <--- demo token
    home-id: 96a14971-525a-4420-aae9-e5aedaa129ff <--- demo house
```
