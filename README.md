thesis
======

You've stumbled on my undergraduate thesis proejct.  This project takes raw Wikipedia articles as input and outputs a knowledge graph wherein "entities" (people, places, events) are nodes and vertices are their commonalities: their connections.  

My dataset consisted of several thousand articles about World War II.  One example entity is the word "Britain" and my algorithm connected it to the following property, expressed as its source sentence: "As agreed with the Allies at the Tehran Conference (November 1943) and the Yalta Conference (February 1945), the Soviet Union entered World War II’s Pacific Theater within three months of the end of the war in Europe".  Note that the word "Britain" does not appear in the sentence, rather my algorithm is able to connect "Britain" as a constituent member of the entity "the Allies" and thus justify its connection to this sentence.

I will fully admit that the code here is not the world's greatest.  I was learning Scala as I built this project.  My long term plan is to rewrite this with a cleaner structure (probably actor-based) and in a more general form.  Someday, hopefully.
