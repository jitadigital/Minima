#!/bin/sh

#The current time
current_time=$(date "+%Y.%m.%d-%H.%M.%S")

#Create a filename
new_fileName="minilogs_"$current_time.txt
echo "Minima Logs File : " "$new_fileName"

#Make sure folder exists
mkdir -p ~/minilogs

#Run it in the background
nohup java -Xmx6G -jar ~/Minima/jar/minima.jar -daemon -connect 10.164.0.3 9001 > ~/minilogs/$new_fileName &

