import json
import sys
import urllib.error
import urllib.request
from decimal import Decimal

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080").rstrip("/") + "/api/v1"


def request(method, path, user=None, body=None):
    headers = {"Content-Type": "application/json"}
    if user is not None:
        headers["X-User-Id"] = str(user)
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as response:
            return json.loads(response.read(), parse_float=Decimal)
    except urllib.error.HTTPError as error:
        raise RuntimeError(f"{method} {path}: HTTP {error.code}: {error.read().decode()}") from error


def check(condition, message):
    if not condition:
        raise RuntimeError(message)


def run():
    book = request("GET", "/instruments/SBER/orderbook")
    check(not book["bids"] and not book["asks"], "SBER order book must be empty before the demo")
    buyer_before = request("GET", "/accounts/101", 1)
    seller_before = request("GET", "/accounts/201", 2)
    positions_before = request("GET", "/accounts/201/positions", 2)
    sber_before = next(p for p in positions_before if p["seccode"] == "SBER")
    check(buyer_before["available"] >= 2020, "Buyer needs at least 2020 RUB available")
    check(sber_before["available"] >= 30, "Seller needs at least 30 SBER available")

    sale = request("POST", "/orders", 2,
                   {"accountId": 201, "seccode": "SBER", "type": "SELL", "price": 100, "quantity": 30})
    check(sale["status"] == "ACTIVE", "Sale should wait in the book")
    buy = request("POST", "/orders", 1,
                  {"accountId": 101, "seccode": "SBER", "type": "BUY", "price": 101, "quantity": 20})
    check(buy["status"] == "FILLED", "Buy should be filled")
    partial = request("GET", f"/orders/{sale['id']}", 2)
    check(partial["status"] == "PARTIALLY_FILLED" and partial["remainingQuantity"] == 10,
          "Sale should have 10 shares remaining")
    book = request("GET", "/instruments/SBER/orderbook")
    check(book["bestAsk"] == 100 and book["asks"][0]["quantity"] == 10, "Wrong best ask")

    trades = request("GET", "/trades?accountId=101&seccode=SBER&size=100", 1)
    trade = next(t for t in trades["items"] if t["buyOrderId"] == buy["id"])
    check(trade["price"] == 100 and trade["quantity"] == 20 and trade["sellOrderId"] == sale["id"],
          "Trade must execute 20 shares at resting price 100")
    register = request("GET", "/orders?accountId=201&seccode=SBER&status=PARTIALLY_FILLED", 2)
    check(any(o["id"] == sale["id"] for o in register["items"]), "Partial sale missing from register")
    cancelled = request("DELETE", f"/orders/{sale['id']}", 2)
    check(cancelled["status"] == "CANCELLED", "Sale remainder should be cancelled")
    buyer_after = request("GET", "/accounts/101", 1)
    seller_after = request("GET", "/accounts/201", 2)
    check(buyer_after["balance"] == buyer_before["balance"] - 2000, "Wrong buyer balance")
    check(buyer_after["reserved"] == buyer_before["reserved"], "Buyer cash reservation leaked")
    check(seller_after["balance"] == seller_before["balance"] + 2000, "Wrong seller balance")
    sber_after = next(p for p in request("GET", "/accounts/201/positions", 2) if p["seccode"] == "SBER")
    check(sber_after["quantity"] == sber_before["quantity"] - 20, "Wrong seller position")
    check(sber_after["reserved"] == sber_before["reserved"], "Seller securities reservation leaked")
    print(f"OK: sale #{sale['id']}, buy #{buy['id']}, trade #{trade['id']}: 20 SBER x 100 RUB")
    print("OK: partial fill, quotes, private registers, cancellation, balances and released reserves")


if __name__ == "__main__":
    run()
