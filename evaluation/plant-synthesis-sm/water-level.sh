index=1
java -Xmx4G -jar ../../jars/plant-automaton-generator.jar ../plant-synthesis/water-level-$index.sc --actionNames abovehh,aboveh,aboveth,abovesp,belowsp,belowth,belowl,belowll,sensorwet,sensordry --eventNames open,closed --ltl ../plant-synthesis/water-level-$index.ltl --actionspec lic100.actionspec --size $((8 * index)) --tree tree.gv --sm 
