# INF8480 TP1 Partie 2

## Pour tout partir

Pour compiler:
'''
ant
'''

Pour envoyer les fichiers sur le serveur :
'''
scp -i /path/to/ssh-key -r /path/to/Partie2 ubuntu@132.207.89.171:/home/ubuntu
'''

Pour se connecter au serveur distant:
'''
ssh -i /path/to/ssh-key ubuntu@132.207.89.171
'''

Pour partir le rmiregistry une fois connecte ssh:
'''
cd /path/to/Partie2;
cd bin;
rmiregistry &
'''

Pour partir le serveur distant une fois le registre rmi parti:
'''
./server.sh 132.207.89.171
'''

Pour partir le client localement une fois que le serveur est parti:
'''
./client.sh 132.207.89.171
'''

## Une fois parti.

Le dossier de reception se cree automatiquement. La liste des usagers est deja cree.
Le fichier de metadonnees contenant qui detient le lock se cree automatiquement aussi.
Le fichier de metadonnees contenant le contenu des groupes est deja cree du cote serveur.

Pour faire la mise a jour de la liste des groupes au niveau client, l'option 10 est disponible. La liste sera alors la meme du cote seveur et client.

Le menu est assez facile d'utilisation et naturel.
