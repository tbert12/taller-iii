from flask import Flask, request
from taxitracker.handler import taxi_handler, travel_handler, billing_handler, stat_handler
import logging

app = Flask(__name__)

app.logger.addHandler(logging.StreamHandler())
app.logger.setLevel(logging.DEBUG)


@app.route('/api/')
def index():
    return "Rest API Taxi Tracker v1"


@app.route('/api/register', methods = ["POST"])
def register():
    return taxi_handler.register_taxi(request)


@app.route('/api/track', methods = ["POST"])
def track():
    return travel_handler.track_travel(request)


@app.route('/api/billing')
def billing():
    return billing_handler.get_billing(request)


@app.route('/api/stats')
def stats():
    return stat_handler.get_stats(request)


@app.route('/api/admin_stats')
def stats_admin():
    return stat_handler.get_admin_stats(request)