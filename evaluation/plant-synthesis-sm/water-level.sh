index=4
java -Xmx4G -jar ../../jars/plant-automaton-generator.jar water-level-$index.sc --actionNames abovehh,aboveh,aboveth,abovesp,belowsp,belowth,belowl,belowll,sensorwet,sensordry --eventNames open,closed --ltl water-level-$index.ltl --actionspec lic100.actionspec --size $((8 * index)) --tree tree.gv --sm 
