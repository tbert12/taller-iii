#/bin/bash
for i in {3..10}; do 
	./up2 radio-listener user${i} radio1 > /tmp/user${i} &
done
