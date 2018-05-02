#/bin/bash
mkdir -p .log
mkdir -p .log/radio-listener
for i in {1..50}; do 
	./up2 radio-listener user${i} radio1 > .log/radio-listener/user${i} &
done

