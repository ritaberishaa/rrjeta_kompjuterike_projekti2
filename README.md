UDP Chat Application

Përshkrimi:

Ky projekt është një aplikacion bisede që përdor protokollin UDP për të komunikuar mes një serveri dhe shumë klientëve në një rrjet të brendshëm.
Aplikacioni lejon dërgimin e mesazheve të tekstit në kohë reale dhe demonstrohet për qëllime edukative dhe demonstruese të komunikimit rrjetor përmes Java.

Karakteristikat:

Komunikim përmes UDP: përdoret UDP për të transmetuar të dhëna, e cila është ideale për skenarët që kërkojnë shpejtësi më të lartë dhe menaxhim më pak të rëndësishëm të sesionit.
Menaxhimi i shumë klientëve: serveri mund të përballojë komunikime të njëkohshme nga shumë klientë, duke u lejuar atyre të ndërveprojnë në rrjet.
Qasje e kufizuar për klientët: klienti i parë që lidhet merr privilegje të plota, ndërsa klientët pasues kanë akses më të kufizuar.

Teknologjitë e përdorura:

Java: gjuha kryesore e programimit për zhvillimin e serverit dhe klientit.

DatagramSocket dhe DatagramPacket: klasa të Java për komunikimin UDP.
