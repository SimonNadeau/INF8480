# INF8480 TP2

## Pour tout partir

Pour compiler:
'''
ant clean
ant
'''

Pour se connecter au serveur distant:
'''
ssh L4712-XX
'''

Pour partir le rmiregistry une fois connecte ssh:
'''
cd /path/to/Partie2;
cd bin;
rmiregistry 5000 &
'''


Il faut aller changer la valeur de la variable ipServeurNoms dans le fichier NomsInterface.java (dans le dossier partage). Cette valeur doit être la meme que l'adresse ip que le serveur de noms. (132.207.12.45).
Elle est utilise par les serveurs de calcul et le repartiteur pour se connecter initialement.



Pour partir le serveur de nom une fois le registre rmi parti:
LOCAL_IP doit être l'ip de la machine.
'''
./noms.sh LOCAL_IP
./noms.sh 132.207.12.45
'''

Pour partir le serveur de calcul une fois le registre rmi parti:
LOCAL_IP doit être l'ip de la machine.
QI est le nombre de ressource
POURCENTAGE est le pourcentage de requete malicieuse.
NOM_SERVEUR est le nom du serveur de calcul differents
'''
./calcul.sh LOCAL_IP QI POURCENTAGE NOM_SERVEUR
./calcul.sh 132.207.12.46 5 0 calcul1
'''

Pour partir le repartiteur localement une fois que le serveur est parti:
LOCAL_IP doit être l'ip de la machine.
FILE_OPERATION est le nom de fichier operation dans le meme directory que le fichier repartiteur.sh
'''
./repartiteur.sh LOCAL_IP FILE_OPERATION
./repartiteur.sh 132.207.12.47 operations-216
'''

## Une fois parti.

En premier lieu, il faut partir le serveur de noms afin que les serveurs de calcul puisse s'y connecter.

Deuxiemement, il faut partir les multiples serveurs de calcul pour qu'il puisse donner leurs infos au serveur de noms.

Troisiement, on part le repartiteur avec le fichier d operations.

Une fois que le client enregistre ses informations, le repartiteur est lance.

Le resultat et le temps final est afficher.
