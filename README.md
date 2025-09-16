# Nemi product manager service

## Hướng dẫn tích hợp API: NhanhVN, Pancake, Sapo

- Tài liệu này hướng dẫn chi tiết các bước khởi tạo ứng dụng, lấy Access Token và gọi API từ 3 nền tảng: **NhanhVN**, **Pancake**, và **Sapo**.
---

## I. NhanhVN
### 1. Tạo và đăng nhập tài khoản
- Tài khoản: `0904858995`  
- Mật khẩu: `Dung@2005`

### 2. Tạo App
- AppID: `76158`  
- BusinessID: `214415`  
- SecretKey: (xem trong app) [NhanhVN App Detail](https://open.nhanh.vn/app/detail?id=76158)  
- Redirect URL: `https://nemi-dev.ecombase.net/redirect_url_1`
![alt text](/docs/images/image.png)

> ⚠️ **Lưu ý**: Tài khoản Nhanh.vn sẽ hết hạn sau 7 ngày, nếu không gia hạn hệ thống sẽ tự động tính phí duy trì

### 3. Lấy Access Code
Truy cập link sau:

```bash
https://nhanh.vn/oauth?version=3.0&appId=76158&returnLink=https://nemi-dev.ecombase.net/redirect_url_1
```

Kết quả trả về (ví dụ):

```
https://nemi-dev.ecombase.net/redirect_url_1?accessCode=0DECuIHCpplrYkvpRoGa441HDsZa0lCUhRA5Gvb128gNcoFT9M7ZUIm9x8gLKvXY
```

> ⚠️ **Lưu ý**: Access code chỉ có hiệu lực **10 phút** và sẽ hết hạn ngay khi đổi sang Access Token.

### 4. Đổi sang Access Token
Request bằng **cURL**:

```bash
curl --location --globoff 'https://pos.open.nhanh.vn/v3.0/app/getaccesstoken?appId={{appId}}&businessId={{businessId}}' --header 'Content-Type: application/json' --data '{
    "accessCode": "ACCESS_CODE",
    "secretKey": "APP_SECRET_KEY"
}'
```

Ví dụ Access Token trả về:

```bash
7HAh4AlUmvO9f67EhWaFFPFdGR0YOM57AGpJSpOONiqn6HDdovYcZaWAo19D2qB9tYZNNfUhUjqtDgBlWu8AhLNlSXw0Oaq11tCETJ1srFNu6nKDecPmAkpSlGYTrJtGQtv8lUqwGUeLEvE8PutVcuo5pDiQqcc9PrI5FznfnfGQj1IPpKOYRUvyufl6tBZ4z7wO6E86zkfCJmDXUPvMNyvetrG6y2MNlQwv6UYh3
```

### 5. Tài liệu API
- Các trạng thái tham khảo: [Model Constant](https://apidocs.nhanh.vn/v3/modelconstant#product-type)

---

## II. Pancake

### 1. Lấy API Key
![alt text](/docs/images/image-1.png)
- API Key: `10b76cf31be245848c8361287cee2adf`

### 2. Gọi API đơn hàng
Ví dụ request:

```bash
https://pos.pages.fm/api/v1/shops/1720119150/orders/8668902978?api_key=10b76cf31be245848c8361287cee2adf
```

Trong đó:
- `{shop_id} = 1720119150`
- `{order_id} = 8668902978`
- `{api_key} = 10b76cf31be245848c8361287cee2adf`

### 3. Proxy qua Localhost
```bash
http://localhost:8080/pancake/1720119150/orders/8668902978
```

Response ví dụ:

```json
{
  "status": "pending",
  "order_id": "22776",
  "order_code": "22776",
  "customer_name": "José Referente",
  "customer_phone": "09982393388",
  "shipping_address": "4INT Rizal Avenue Pob2 Nagcarlan Laguna back municipal building , Poblacion ii (pob.), Nagcarlan, Laguna",
  "payment_method": "COD",
  "shipping_method": "Standard",
  "total_price": 649.0,
  "shipping_fee": 0.0,
  "discount_amount": 0.0,
  "created_at": "2025-09-02T20:09:01.587660",
  "updated_at": "2025-09-03T10:13:45.370008"
}
```

---

## III. Sapo

### 1. Lấy chứng chỉ Client
Truy cập: [Sapo Developer - API Clients](https://developers.sapo.vn/services/partners/api_clients)
![alt text](/docs/images/image-2.png)
![alt text](/docs/images/image-3.png)
Ví dụ:
- API key: `50f184d93c834ebaa764cc17c25e31fa`
- Secret key: `50ce6b4d47a04ca18aa6f972b698779a`

Tạo shop tại: [Dev Shop](https://developers.sapo.vn/services/partners/dev_shop)  
![alt text](/docs/images/image-4.png)
- Shop: `Nemi`  
- Tài khoản: `nncuong377@gmail.com`  
- Mật khẩu: `Cuong?432000`

### 2. Xin cấp quyền (Authorization)
Redirect user đến URL:

```bash
https://{store}.mysapo.net/admin/oauth/authorize?client_id={api_key}&scope={scopes}&redirect_uri={redirect_uri}
```

Ví dụ với store `nemi`:

```bash
https://nemi.mysapo.net/admin/oauth/authorize?client_id=50f184d93c834ebaa764cc17c25e31fa&scope=read_content,write_content,read_themes,write_themes,read_products,write_products,read_customers,write_customers,read_orders,write_orders,read_script_tags,write_script_tags,read_price_rules,write_price_rules,read_draft_orders,write_draft_orders&redirect_uri=https://nemi-dev.ecombase.net/
```

![alt text](/docs/images/image-6.png)

Khi người dùng đồng ý, hệ thống sẽ redirect về:

```bash
https://nemi-dev.ecombase.net/?code=1dce6443b2284b7185e39d8065d5cd88&hmac=xxx&store=nemi.mysapo.net&timestamp=1757243040
```

- `code`: Authorization Code (dùng để lấy Access Token)

### 3. Lấy Access Token
Dùng `code`, `client_id`, và `client_secret`:

```bash
curl --location --request POST 'https://nemi.mysapo.net/admin/oauth/access_token?client_id=50f184d93c834ebaa764cc17c25e31fa&client_secret=50ce6b4d47a04ca18aa6f972b698779a&code=104ae3ce8c754b509e510021d660d3d4'
```

Response:

```json
{
  "access_token": "59d0c4eea0fc497e81733f693d3e4641",
  "scope": "read_content read_customers read_draft_orders read_orders read_price_rules read_products read_script_tags read_themes write_content write_customers write_draft_orders write_orders write_price_rules write_products write_script_tags write_themes"
}
```
![alt text](/docs/images/image-7.png)

👉 Access Token có hiệu lực **vĩnh viễn**.

### 4. Tạo request xác thực
Tất cả request gửi kèm header:

```http
X-Sapo-Access-Token: {access_token}
```


---

# Kết luận

- **NhanhVN**: OAuth 2 bước (Access Code → Access Token, ngắn hạn).  
- **Pancake**: Chỉ cần API Key, gọi trực tiếp.  
- **Sapo**: OAuth, nhưng Access Token vĩnh viễn.  




# Webhook Hands-on

### Nhanh.vn
- Workflow Diagram (Nhanh.vn) 
```lua
+-------------------+
|   App in Nhanh.vn  |
|   (Enable Webhooks)|
+---------+----------+
          |
          | 1. Admin bật Webhooks: 
          |    - cấu hình callback URL (HTTPS, POST) 
          |    - nhập verify token
          |    - chọn event types (productAdd, orderUpdate, etc.)
          v
+---------------------------+
|  Nhanh.vn system          |
|---------------------------|
| - gửi event "webhooksEnabled"  --> tests URL, kèm verify token  |
+---------------------------+
          |
          | 2. Khi sự kiện phát sinh (ví dụ: SP mới, đơn mới, cập nhật SP/ĐH,…)
          v
+---------------------------+
|  Webhook POST JSON        |
|  tới callback URL         |
|  Nội dung có:             |
|     - event name          |
|     - webhooksVerifyToken |
|     - data (payload)      |
+---------------------------+
          |
          | 3. Server bạn nhận payload
          |    - verify token match
          |    - xử lý dữ liệu phù hợp event
          v
+---------------------------+
|  Your backend / App       |
|  - Xử lý, mapping dữ liệu | |
+---------------------------+
          ^
          |
          | Nếu HTTP status != 200 hoặc timeout → Nhanh.vn retry tới 3 lần
          |
+---------------------------+
|  Callback response 200    |
+---------------------------+
```

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
![alt text](./docs/images/pancake-webhook.png)
- Hiện tại thì pancake chỉ hỗ trợ được 3 loai dữ liệu: Đơn hàng(Order), Khách Hàng(Customers), Tồn kho (Inventory)

- Workflow Diagram (Pancake)
```lua
+---------------------------+
| Pancake POS / Pancake System |
| (Settings → Webhook setup)   |
+-------------+-------------+
              |
              | 1. Admin cài đặt:
              |    - nhập callback URL
              |    - chọn event types cần nhận
              |    
              v
+-----------------------------+
| Pancake system              |
+-----------------------------+
              |
              | 2. Khi event xảy ra (order mới, cập nhật đơn, thay đổi trạng thái,…)
              v
+-----------------------------+
| HTTP POST tới callback URL   |
| Payload chứa event + data    |
| Có thể test qua webhook UI   |
+-----------------------------+
              |
              | 3. Server bạn nhận POST
              |    - verify token nếu có
              |    - xử lý payload
              |    - trả về HTTP status 200 nếu thành công
              v
+-----------------------------+
| Your backend / DB / App       |
+-----------------------------+
              ^
              |
              | Nếu HTTP status !=200 hoặc timeout thì Pancake có retry (tùy event / cấu hình)
              |
+-----------------------------+
| Callback Response 200         |
+-----------------------------+
```

- **Cấu hình webhook trên app:**

![Pancake Webhook Config](./docs/images/pancake-config.png)
1. Add Order
```http
curl --location 'https://pos.pages.fm/api/v1/shops/1720119150/orders?api_key=10b76cf31be245848c8361287cee2adf' \
--header 'Content-Type: application/json' \
--data '{
  "bill_full_name": "Test Customer",
  "bill_phone_number": "09123456789",
  "is_free_shipping": false,
  "received_at_shop": false,
  "page_id": "1234191921173353",
  "shop_id": 1720119150,
  "warehouse_id": "410404e4-3d4c-4674-8081-e6f1334cd344",
  "items": [
    {
      "quantity": 1,
      "product_id": "ec96ab3b-e5aa-4050-bf96-7f0340695ba4",
      "variation_id": "de9e281f-137a-413f-9ae2-f2b792bcb902"
    }
  ],
  "shipping_address": {
    "full_name": "Test Customer",
    "phone_number": "09123456789",
    "address": "Test Address",
    "full_address": "Test Address, Santa ana, Taguig, Metro-manila",
    "province_id": "63219",
    "district_id": "632191611",
    "commune_id": "6321916115836"
  },
  "shipping_fee": 0,
  "total_discount": 0,
  "custom_id": "TEST008"
}
'
```

2. Update Order
```http
curl --location --request PUT 'https://pos.pages.fm/api/v1/shops/1720119150/orders/TEST007?api_key=10b76cf31be245848c8361287cee2adf' \
--header 'Content-Type: application/json' \
--data '{
    "status": 1,
    "shipping_fee": 50000,
    "bill_full_name": "Test Customer Updated",
    "bill_phone_number": "09999999999"
  }'
```

---

### Sapo
- Đăng ký webhook cho từng chức năng (topic), callback url là **address**
![Sapo Webhook Config](./docs/images/sapo-config.png)

- Workflow Diagram (Sapo)
```lua
+-------------------+
|   Sapo Admin / App |
|   (tạo Webhook)    |
+---------+----------+
          |
          | 1. Request tạo webhook:
          |    - chọn event(s) 
          |    - định dạng (json/xml)
          |    - URL nhận sự kiện (address)
          v
+---------------------------+
|  Sapo system              |
+---------------------------+
          |
          | 2. Khi event xảy ra (ví dụ: order.create, product.update, refund.create,…)
          v
+---------------------------+
|  Webhook POST             |
|  gửi tới address          |
|  Payload event + data     |
+---------------------------+
          |
          | 3. Your server nhận POST
          |    - parse JSON/XML
          |    - respond HTTP status 200 nếu ok
          |    
          v
+---------------------------+
|  Your backend / DB / App   |
+---------------------------+
          ^
          |
          | Nếu trả về ≠200 hoặc lỗi → Sapo có thể retry (tuỳ event / thiết lập)
          |
+---------------------------+
| Callback Response 200      |
+---------------------------+

```

- **Call API tạo mới webhook:**

```http
curl --location 'https://nemi.mysapo.net/admin/webhooks.json' \
--header 'X-Sapo-Access-Token: 59d0c4eea0fc497e81733f693d3e4641' \
--data '{
  "webhook": {
    "topic": "orders/create",
    "address": "https://nemi-dev.ecombase.net/",
    "format": "json"
  }
}'
```

- **Response tạo webhook thành công:**

```
HTTP/1.1 201 Created
{
    "webhook": {
        "id": 1451977,
        "address": "https://nemi-dev.ecombase.net/",
        "format": "json",
        "topic": "orders/create",
        "created_on": "2025-09-08T15:12:51Z",
        "modified_on": "2025-09-08T15:12:51Z"
    }
}
```

1. **Product**
- Add product:
```http
curl --location 'https://nemi.mysapo.net/admin/products.json' \
--header 'X-Sapo-Access-Token: 59d0c4eea0fc497e81733f693d3e4641' \
--header 'Content-Type: application/json' \
--data '{
  "product": {
    "name": "Burton Custom Freestlye 151",
    "content": "<strong>Good snowboard!<\/strong>",
    "vendor": "Burton",
    "product_type": "Snowboard",
    "images": [
      {
        "src": "http:\/\/example.com\/rails_logo.gif"
      }
    ]
  }
}'
```

- Update Product:
```http
curl --location --request PUT 'https://nemi.mysapo.net/admin/products/58423224.json' \
--header 'X-Sapo-Access-Token: 59d0c4eea0fc497e81733f693d3e4641' \
--header 'Content-Type: application/json' \
--data '{
  "product": {
    "id": 58423224,
    "name": "New NAME"
  }
}'
```

- Delete Product:
```http
curl --location --request DELETE 'https://nemi.mysapo.net/admin/products/58423627.json' \
--header 'X-Sapo-Access-Token: 59d0c4eea0fc497e81733f693d3e4641'
```

2. **Order**
- Add Order
```http
curl --location 'https://nemi.mysapo.net/admin/orders.json' \
--header 'X-Sapo-Access-Token: 59d0c4eea0fc497e81733f693d3e4641' \
--header 'Content-Type: application/json' \
--data-raw '{
  "order": {
    "email": "foo@example.com",
    "send_receipt": true,
    "send_fulfillment_receipt": true,
    "line_items": [
      {
        "variant_id": 166621634,
        "quantity": 1
      }
    ]
  }
}'
```

- Update Order
```http
curl --location --request PUT 'https://nemi.mysapo.net/admin/orders/54918075.json' \
--header 'X-Sapo-Access-Token: 59d0c4eea0fc497e81733f693d3e4641' \
--header 'Content-Type: application/json' \
--data '{
  "order": {
    "id": 450789469,
    "note": "Customer contacted us about a custom engraving on this iPod."
  }
}'
```

- Delete Order
```http
curl --location --request DELETE 'https://nemi.mysapo.net/admin/orders/54918075.json' \
--header 'X-Sapo-Access-Token: 59d0c4eea0fc497e81733f693d3e4641'
```
