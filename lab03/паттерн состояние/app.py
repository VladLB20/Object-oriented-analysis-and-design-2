from flask import Flask, render_template, request, jsonify
from delivery_logic import Order, Messenger

app = Flask(__name__)

order = Order(
    order_id="OZ-2025",
    customer="Мария Иванова",
    warehouse="Склад №5",
    courier="Антон Смирнов",
    delivery_address="Москва, ул. Ленина, 10"
)

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/api/status')
def status():
    return jsonify({
        "order": order.to_dict(),
        "notifications": Messenger.get_all()
    })

@app.route('/api/route', methods=['POST'])
def update_route():
    data = request.get_json()
    points = data.get('points', [])
    try:
        order.set_route(points)
        Messenger.clear()
        return jsonify({"success": True})
    except ValueError as e:
        return jsonify({"success": False, "error": str(e)}), 400

@app.route('/api/next', methods=['POST'])
def next_step():
    try:
        order.next()
        return jsonify({"success": True, "status": order.get_status()})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 400

@app.route('/api/cancel', methods=['POST'])
def cancel_order():
    try:
        order.cancel()
        return jsonify({"success": True, "status": order.get_status()})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 400

if __name__ == '__main__':
    app.run(debug=True)