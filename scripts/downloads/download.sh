cd /Users/yf/git/dataonehk/scripts/downloads
rm -f latest-speedmap.xml
curl -s resource.data.one.gov.hk/td/speedmap.xml > latest-speedmap.xml
md5 -q latest-speedmap.xml > latest-speedmap.md5
if [ -f "current-speedmap.xml" ]; 
then
   if cmp -s "latest-speedmap.md5" "current-speedmap.md5"; 
   	then
      echo ""
	else
	  cp -f latest-speedmap.xml current-speedmap.xml
	  cp -f latest-speedmap.md5 current-speedmap.md5    
      echo ""
	fi

else
	cp -f latest-speedmap.xml current-speedmap.xml
	cp -f latest-speedmap.md5 current-speedmap.md5    
	echo ""
fi


#rm -f latest-speedmap.json
#curl -s http://govapi.amoebaconsulting.net/api/traffic/speedmap/latest > latest-speedmap.json
#md5 -q latest-speedmap.json > latest-speedmap-json.md5
#if [ -f "current-speedmap.json" ]; 
#then

#   if cmp -s "latest-speedmap-json.md5" "current-speedmap-json.md5"; 
#   	then
#		echo ""      
#	else
#	  cp -f latest-speedmap.json current-speedmap.json
#	  cp -f latest-speedmap-json.md5 current-speedmap-json.md5    
#      echo ""
#	fi

#else
#	cp -f latest-speedmap.json current-speedmap.json
#	cp -f latest-speedmap-json.md5 current-speedmap-json.md5    
#	echo ""
#fi


