#!/bin/bash

# Update all premium URLs in the ButtonListener.java file
sed -i 's|https://deadside.com/premium|https://emeraldskillfeed.tip4serv.com/|g' src/main/java/com/deadside/bot/listeners/ButtonListener.java
sed -i 's|https://deadside.com/premium|https://emeraldskillfeed.tip4serv.com/|g' com/deadside/bot/listeners/ButtonListener.java
sed -i 's|https://deadside.com/premium|https://emeraldskillfeed.tip4serv.com/|g' com/deadside/bot/premium/FeatureGate.java

echo "Updated premium links to point to Tip4serv website"