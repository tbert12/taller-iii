from flask import jsonify

def json_response(json_data):
    json_data['_info_'] = {
        'api' : 'TaxiTracker',
        'version' : 1.0
    }
    return jsonify(json_data)

def response_from_boolean(boolean, info = ""):
    code = 200 if boolean else 400
    return json_response({
        'success' : boolean,
        'info' : info,
    }), code

def response_from_json(json):
    return json_response(json), 200
