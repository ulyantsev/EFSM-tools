index=2
java -Xmx4G -jar ../../jars/plant-automaton-generator.jar water-level-$index.sc --actionNames abovehh,aboveh,aboveth,abovesp,belowsp,belowth,belowl,belowll,sensorwet,sensordry --actionNumber 10 --eventNames open,closed --eventNumber 2 --ltl water-level-$index.ltl --actionspec lic100.actionspec --size $((8 * index)) --varNumber 0 --tree tree.gv 
