#!/bin/sh

#The current time
current_time=$(date "+%Y.%m.%d-%H.%M.%S")

#Create a filename
new_fileName="minilogs_"$current_time.txt
echo "New Logs FileName: " "$new_fileName"

#Make sure exists
mkdir -p ~/minilogs

#Run it in the background
nohup java -jar ~/Minima/jar/minima.jar -clean -private -daemon > ~/minilogs/$new_fileName &


