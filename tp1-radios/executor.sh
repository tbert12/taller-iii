#/bin/bash
for i in {3..10}; do 
	./up radio-listener user${i} radio1 > /tmp/user${i} &
done

