task = dict(
    travel_counter = dict(
        QUEUE_NAME = 'travels-count-queue',
        LEASE_TASK_COUNT = 5000,
        LEASE_TASK_TIME_SEC = 50,
    ),
    billing_accumulator = dict(
        QUEUE_NAME = 'billing-queue',
        LEASE_TASK_COUNT = 5000,
        LEASE_TASK_TIME_SEC = 50,
    ),
)

store = dict(
    MEMCACHE_BILLING_EXPIRATION_SEC = 120,
    MEMCACHE_STATS_EXPIRATION_SEC = 60*60,
    DEFAULT_PAGE_SIZE = 10
)