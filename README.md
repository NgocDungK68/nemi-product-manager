# Nemi product manager service

## Webhook Hands-on

### Nhanh.vn

- **Cấu hình webhook trên app:**

![Nhanhvn Webhook Config](./docs/images/nhanhvn-config.png)

➡️ Sau khi cấu hình thành công sẽ nhận response **`webhooksEnabled`**

- **Webhook data:**
    - `event` (string): Tên sự kiện
    - `businessId`
    - `data` (json string)

![Nhanhvn Webhook Data](./docs/images/nhanhvn-data.png)

---

### Pancake

- **Cấu hình webhook trên app:**

![Pancake Webhook Config](./docs/images/pancake-config.png)

---

### Sapo

- **Call API tạo mới webhook:**

```http
POST /admin/webhooks.json
{
  "webhook": {
    "topic": "orders/create",
    "address": "http://whatever.hostname.com/",
    "format": "json"
  }
}
```

- **Response tạo webhook thành công:**

```
HTTP/1.1 201 Created
{
  "webhook": {
    "id": 987911590,
    "address": "http://whatever.hostname.com/",
    "topic": "orders/create",
    "created_on": "2016-01-20T13:01:10Z",
    "modified_on": "2016-01-20T13:01:10Z",
    "format": "json"
  }
}
```