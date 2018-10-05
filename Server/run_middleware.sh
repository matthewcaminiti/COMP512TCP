./run_rmi.sh > /dev/null

#echo "Edit file run_middleware.sh to include instructions for launching the middleware"
#echo '  $1 - hostname of Flights'
#echo '  $2 - hostname of Cars'
#echo '  $3 - hostname of Rooms'

java -cp "/home/2017/mcamin/Documents/TemplateTCP/Server/server/ecafretni:/home/2017/mcamin/Documents/TemplateTCP/Server/" server.tcp.MiddlewareServer $1 $2 $3 $4