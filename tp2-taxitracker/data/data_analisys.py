import pandas as pd

DF_TRAVELS = pd.read_csv('2015-12_100k.csv')
print "Headers"
print list(DF_TRAVELS)
print ""

# Clean data base
IGNORED_COLS = ['RateCodeID', 'store_and_fwd_flag', 'payment_type', 'fare_amount', 'extra', 
    'mta_tax', 'tip_amount', 'tolls_amount', 'improvement_surcharge', 
    'pickup_zip', 'pickup_borough', 'pickup_neighborhood', 'dropoff_zip', 
    'dropoff_borough', 'dropoff_neighborhood'
]
for col in IGNORED_COLS:
    del DF_TRAVELS[col]

print "New Headers"
print list(DF_TRAVELS)
print ""

# Pandas stats
print DF_TRAVELS.sample(n=5)

# Count travels per vender id
from collections import defaultdict
VENDOR_IDS = defaultdict(int)
for index, row in DF_TRAVELS.iterrows():
    VENDOR_IDS[row['VendorID']] += 1

print "Total drivers: {}".format(len(VENDOR_IDS))

print "Travels per driver:"
for vendor in VENDOR_IDS.keys():
    print "VendorID: {} - Travels: {}".format(vendor, VENDOR_IDS[vendor])



