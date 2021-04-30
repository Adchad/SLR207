# SLR207 - Rapport

L’objectif de ce projet est de créer une implémentation de MapReduce en JAVA, dans une architecture répartie. Le but sera ensuite de comparer les performances d’une architecture répartie avec une architecture séquentielle, et ensuite d’essayer de prouver empiriquement la loi d’Amdahl.


## Première étape : implémentation séquentielle.

La version séquentielle de l’algorithme est assez simple à implémenter. Il s’agit de parcourir le texte à analyser, et compter le nombre d'occurrences de chaque mot via une Map. La difficulté de cette architecture ne vient pas de l'implémentation, mais de la performance, qui est parfois si mauvaise que cela rend l'expérience impossible, pour des fichiers trop grands par exemple, je n’ai pas réussi à obtenir de résultat car ça prenait trop de temps.


## Deuxième étape : implémentation distribuée.

Le premier problème que j’ai rencontré est la connexion aux processeurs à distance, qui s’est révélée être un vrai problème. En particulier, pour l'exécution des commandes à distance, via ProcessBuilder et ssh est très aléatoire, et à ce jour je n’ai pas réussi à éviter des timeouts de certaines commandes à certains endroits. Par exemple, lors de la première connexion à un ordinateur distant après longtemps, il y a presque tout le temps un timeout, c’est pourquoi je recommande lors du test de mon programme d'exécuter deux fois de suite le CLEAN.jar.

Dans l’ordre du mapReduce, la première étape qui m’a posé beaucoup de difficultés est le **splitting**. (dans le code MASTER, fonction splitFile (l.99))

Le but ici est de découper le fichier à mapreduce en le nombre de process disponibles. Mais si l’on découpe le fichier en le parcourant, on risque de prendre trop de temps inutilement, et ça peut devenir vraiment long pour des fichiers très gros. L’idée est donc de découper le fichier en divisant le nombre d’octets du fichier par le nombre de process. Seulement, on ne doit pas découper les mots, et on doit donc découper à l’espace le plus proche.
Mon implémentation repose donc sur un offset, qui indique le décalage impliqué par les espaces, et on parcours le fichier de taille de split en taille de split.


Ensuite vient l’étape du **map**, qui ne m’a pas posé vraiment de problème, car il s’agit juste de parcourir le fichier et d’associer chaque mot à la valeur 1.

L’étape qui a été la plus compliquée pour moi est le **shuffle**  dans le code SLAVE, fonction shuffle (l.80)),

d’abord, j’ai eu une petite difficulté à comprendre que la fonction hashcode() pouvait renvoyer un entier négatif, ce qui a perturbé pendant longtemps l'exécution.
Au-delà de ça, le vrai problème que j’ai rencontré lors du shuffle, est l’étape juste après, qui consiste en le fait d’envoyer les fichiers contenant les mots aux autres process. On se retrouve vite avec un très grand nombre de fichiers (il y a presque un fichier par mot), et pour moi, cela prend un temps très très grand pour envoyer ces fichiers (plus de 15 minutes pour des gros fichiers d’input), car il faut établir une connexion scp pour chaque fichier envoyé, ce qui prend énormément de temps.

Les étapes suivantes : **reduce** et **gather** ne m’ont pas posé de problèmes, il suffisait de parcourir les fichiers reçus et de rassembler les mots entre eux.


## Troisième étape : Passage à grande échelle

C’est l’étape qui m’a causé le plus de problèmes, notamment à cause du problème que j’ai rencontré dans le shuffle, que j’ai expliqué précédemment. Pour les fichiers de taille assez importante, le temps que prend l’envoi du très grand nombre de fichiers shuffles générés prend un temps beaucoup trop grand.

Je n'ai pas à ce jour réussi à résoudre ce problème (j'espère pouvoir le faire à l'avenir), ce qui ma empéché de pouvoir faire des experiences intéressantes, notament d'essayer de prouver la loi d'Ahmdal.
En effet les seuls résultats que j'ai pu obtenir sont ceux pour le fichier input de test (Car Car Beer ...), cette experience est disponible dans le dossier **SimpleInput**. Malheureusement, cette experience ne permet pas de prouver grand chose : la méthode séquentielle prend beaucoup moins de temps que la méthode distribuée ( 1 seconde contre environ 10 secondes), car le temps de connexion ssh est beaucoup plus grand que le temps de calcul.

