#!/bin/csh -f

# The 20 newsgroups documents are traditional 8 bit not utf-8
setenv LC_ALL iso-8859-1
foreach dataset (20news-bydate-train 20news-bydate-test)
  set output="$dataset-stanford-classifier-iso-8859-1.txt"
  rm -f $output
  foreach newsgroup ($dataset/*)
    foreach file ($newsgroup/*)
      set cls=`echo $newsgroup | cut -d "/" -f 2`
      printf "$cls\t" >> $output
      tr '\n\r\t' '   '< $file  | tr -d '\377' >> $output
      printf "\n" >> $output
    end
  end
  iconv -f iso-8859-1 -t utf-8 < $output > $dataset-stanford-classifier.txt
end
