from flask import Flask, request, jsonify

app = Flask(__name__)

last_trigger = {
    "triggered": False
}

@app.route("/trigger", methods=["POST"])
def trigger():
    data = request.get_json(force=True)
    last_trigger["triggered"] = data.get("triggered", False)

    print("Received trigger:", last_trigger["triggered"])

    return jsonify({"ok": True, "triggered": last_trigger["triggered"]})

@app.route("/state", methods=["GET"])
def state():
    return jsonify(last_trigger)

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)