#!/bin/bash

ip=$1
host=$2

base_dir="testes_vetores"
mkdir $base_dir

file="50kb"
dir="$base_dir/$file/"
mkdir $dir
java -Djava.rmi.server.hostname=$ip br.inf.ufes.ppd.ClienteTestes $host $file.txt.cipher ipsum 5
mv analise_cliente.csv $dir
mv analise_cliente_mean.csv $dir

file="60kb"
dir="$base_dir/$file/"
mkdir $dir
java -Djava.rmi.server.hostname=$ip br.inf.ufes.ppd.ClienteTestes $host $file.txt.cipher ipsum 5
mv analise_cliente.csv $dir
mv analise_cliente_mean.csv $dir

file="70kb"
dir="$base_dir/$file/"
mkdir $dir
java -Djava.rmi.server.hostname=$ip br.inf.ufes.ppd.ClienteTestes $host $file.txt.cipher ipsum 5
mv analise_cliente.csv $dir
mv analise_cliente_mean.csv $dir

file="80kb"
dir="$base_dir/$file/"
mkdir $dir
java -Djava.rmi.server.hostname=$ip br.inf.ufes.ppd.ClienteTestes $host $file.txt.cipher ipsum 5
mv analise_cliente.csv $dir
mv analise_cliente_mean.csv $dir

file="90kb"
dir="$base_dir/$file/"
mkdir $dir
java -Djava.rmi.server.hostname=$ip br.inf.ufes.ppd.ClienteTestes $host $file.txt.cipher ipsum 5
mv analise_cliente.csv $dir
mv analise_cliente_mean.csv $dir

file="100kb"
dir="$base_dir/$file/"
mkdir $dir
java -Djava.rmi.server.hostname=$ip br.inf.ufes.ppd.ClienteTestes $host $file.txt.cipher ipsum 5
mv analise_cliente.csv $dir
mv analise_cliente_mean.csv $dir

file="110kb"
dir="$base_dir/$file/"
mkdir $dir
java -Djava.rmi.server.hostname=$ip br.inf.ufes.ppd.ClienteTestes $host $file.txt.cipher ipsum 5
mv analise_cliente.csv $dir
mv analise_cliente_mean.csv $dir

file="120kb"
dir="$base_dir/$file/"
mkdir $dir
java -Djava.rmi.server.hostname=$ip br.inf.ufes.ppd.ClienteTestes $host $file.txt.cipher ipsum 5
mv analise_cliente.csv $dir
mv analise_cliente_mean.csv $dir

file="130kb"
dir="$base_dir/$file/"
mkdir $dir
java -Djava.rmi.server.hostname=$ip br.inf.ufes.ppd.ClienteTestes $host $file.txt.cipher ipsum 5
mv analise_cliente.csv $dir
mv analise_cliente_mean.csv $dir

file="140kb"
dir="$base_dir/$file/"
mkdir $dir
java -Djava.rmi.server.hostname=$ip br.inf.ufes.ppd.ClienteTestes $host $file.txt.cipher ipsum 5
mv analise_cliente.csv $dir
mv analise_cliente_mean.csv $dir

file="150kb"
dir="$base_dir/$file/"
mkdir $dir
java -Djava.rmi.server.hostname=$ip br.inf.ufes.ppd.ClienteTestes $host $file.txt.cipher ipsum 5
mv analise_cliente.csv $dir
mv analise_cliente_mean.csv $dir

