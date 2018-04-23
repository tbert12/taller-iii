from flask import Flask, jsonify
import json

app = Flask(__name__)
app.config['APPLICATION_ROOT'] = "/rest"

@app.route('/register', method = ["POST"])
def register():
    if request.method == "POST":
        # Create taxi. Arguments id or generate id
        return jsonify({
            success : True
            taxi_id : 456
        })
    return jsonify({
        success : False
        info : "Request method should be POST"
    }), 404

@app.route('/track', method = ["POST"])
def track():
    if request.method == "POST":
        # Track taxi. taxi_id, start, end, price 
        return jsonify({
            success : True
        })
    return jsonify({
        success : False
        info : "Request method should be POST"
    }), 404

@app.route('/billing')
def billing():
    # Return day billig of taxi_id
    return jsonify({
        taxi_id: 23
        total: 778
    })

@app.route('/stats')
def stats():
    # Travels per day
    return jsonify({
        stats: [
            {
                day: '23-04-2018'
                travels : 53366
            }
        ]
    })


@app.route('/admin_stats')
def stats():
    # Gets data from filter
    return jsonify({
        stats : [
            taxi_id : 123
            travel : {}
        ]
    })