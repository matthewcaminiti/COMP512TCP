# Usage: ./run_client.sh [<server_hostname> [<server_rmiobject>]]

#"$(echo $SSH_CONNECTION | awk '{ print $4 }')"
#java -Djava.security.policy=java.policy -cp ../Server/RMIInterface.jar:. Client.RMIClient $1 $2
#java client.TCPClient "$(echo $SSH_CONNECTION | awk '{ print $3 }')"
java client.TCPClient lab1-2.cs.mcgill.ca
