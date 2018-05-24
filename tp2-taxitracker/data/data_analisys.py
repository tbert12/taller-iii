import pandas as pd
import datetime
import random

TOTAL_VENDORS = 50
MAX_VENDOR_ID_VALUE = 200
LIMIT_DATAFRAME = 5000

DF_TRAVELS = pd.read_csv('2015-12_100k.csv')
print "Headers"
print list(DF_TRAVELS)
print ""

# 1. Take important cols
COLS = ['VendorID', 'pickup_datetime', 'dropoff_datetime', 'passenger_count', 'trip_distance', 'pickup_longitude', 'pickup_latitude', 'dropoff_longitude', 'dropoff_latitude', 'total_amount']

DF_TRAVELS = DF_TRAVELS[COLS] 

print "New Headers"
print list(DF_TRAVELS)
print ""

# 2. Take only 5K
DF_TRAVELS = DF_TRAVELS.sample(n=LIMIT_DATAFRAME)

VENDORS_ID = [ random.randint(1,MAX_VENDOR_ID_VALUE) for _ in xrange(TOTAL_VENDORS) ]
# 3. Generate random VENDORS (because there are 2 in file)
DF_TRAVELS['VendorID'] = DF_TRAVELS['VendorID'].apply(lambda x: random.choice(VENDORS_ID))

print "Total drivers: ", len(DF_TRAVELS.groupby("VendorID"))

# Generate datframe to TEST
data = [
    #  { vendorID | Type | Date | latitude | longitude | amount }
]

for _, row in DF_TRAVELS.iterrows():
    data.append({
        "vendorID" : row["VendorID"],
        "date" : row["pickup_datetime"],
        "type" : "PICKUP",
        "amount" : None,
        "latitude" : row["pickup_latitude"],
        "longitude" : row["pickup_longitude"]
    })

    data.append({
        "vendorID" : row["VendorID"],
        "date" : row["dropoff_datetime"],
        "type" : "DROPOFF",
        "amount" : row["total_amount"],
        "latitude" : row["dropoff_latitude"],
        "longitude" : row["dropoff_longitude"]
    })
    
      



now = datetime.datetime.now()
def to_today(x):
    x = x.replace(year=now.year, month=now.month, day=now.day)
    return x


DF_TEST = pd.DataFrame(data)
DF_TEST = DF_TEST[["vendorID","date","type","amount","latitude","longitude"]]
DF_TEST['date'] = pd.to_datetime(DF_TEST.date)
DF_TEST['date'] = DF_TEST['date'].apply(lambda x: to_today(x))
DF_TEST.sort_values(by='date')
first_date = DF_TEST.iloc[0]["date"]
DF_TEST['difference'] = DF_TEST['date'].apply(lambda x: pd.Timedelta(x.to_pydatetime() - first_date.to_pydatetime()).seconds)
DF_TEST.to_csv("test.csv", index=False)



