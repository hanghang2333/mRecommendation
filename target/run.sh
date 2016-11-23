#!/usr/bin/env bash
spark-submit --class model.model --jars ansj_seg-5.0.1.jar,tree_split-1.4.jar,nlp-lang-1.7.1.jar recommendation-1.0-SNAPSHOT.jar