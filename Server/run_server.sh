#Usage: ./run_server.sh [<rmi_name>]

#./run_rmi.sh > /dev/null 2>&1
#java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd)/ server.tcp.RMServer
java -cp "/home/2017/mcamin/Documents/TemplateTCP/Server/server/ecafretni:/home/2017/mcamin/Documents/TemplateTCP/Server/" server.tcp.RMServer $1
#java -cp "/Users/matthewcaminiti/Documents/TemplateTCP/Server/server/ecafretni:/Users/matthewcaminiti/Documents/TemplateTCP/Server/" server.tcp.RMServer $1
