#!/bin/sh
# Compila o modelo Java e sobe o servidor da partida.
set -e
cd "$(dirname "$0")"

PORTA="${1:-8080}"

echo "Compilando..."
mkdir -p build
javac -d build $(find src/java -name '*.java')

echo "Subindo o servidor na porta $PORTA..."
exec java -cp build carcassonne.Principal --porta "$PORTA"
