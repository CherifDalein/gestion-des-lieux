#!/bin/bash

# GestionDesLieux - Script de compilation et execution
# Usage: ./run.sh [compile|test|package|run|clean|all]

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    GRADLE="./gradlew"
    echo -e "${BLUE}[INFO] Utilisation du Gradle Wrapper${NC}"
elif command -v gradle >/dev/null 2>&1; then
    GRADLE="gradle"
    echo -e "${BLUE}[INFO] Utilisation de Gradle systeme${NC}"
else
    echo -e "${RED}[ERREUR] Ni ./gradlew ni Gradle ne sont disponibles!${NC}"
    echo ""
    echo "Solutions :"
    echo "  1. Utilisez le Gradle Wrapper inclus dans le projet"
    echo "  2. Ou installez Gradle : https://gradle.org/install/"
    echo ""
    exit 1
fi

print_header() {
    echo -e "${GREEN}================================================${NC}"
    echo -e "${GREEN}  GestionDesLieux - Spring Boot Application${NC}"
    echo -e "${GREEN}================================================${NC}"
    echo ""
}

find_jar() {
    local jar_path=""
    local file

    for file in build/libs/*.jar; do
        [ -e "$file" ] || continue
        case "$file" in
            *-plain.jar) ;;
            *)
                jar_path="$file"
                break
                ;;
        esac
    done

    if [ -z "$jar_path" ]; then
        return 1
    fi

    printf '%s\n' "$jar_path"
}

compile_app() {
    echo -e "${YELLOW}[1/1] Compilation du projet...${NC}"
    "$GRADLE" clean classes -x test
    echo -e "${GREEN}[OK] Compilation reussie${NC}"
    echo ""
}

test_app() {
    echo -e "${YELLOW}[1/1] Execution des tests...${NC}"
    "$GRADLE" test
    echo -e "${GREEN}[OK] Tests executes avec succes${NC}"
    echo ""
}

package_app() {
    local jar_path

    echo -e "${YELLOW}[1/1] Creation du JAR executable...${NC}"
    "$GRADLE" clean bootJar -x test
    jar_path="$(find_jar)"
    echo -e "${GREEN}[OK] JAR cree : ${jar_path}${NC}"
    echo ""
}

run_app() {
    local jar_path

    echo -e "${YELLOW}Demarrage de l'application...${NC}"
    echo ""

    if jar_path="$(find_jar 2>/dev/null)"; then
        java -jar "$jar_path"
    else
        echo -e "${YELLOW}JAR introuvable, compilation en cours...${NC}"
        package_app
        jar_path="$(find_jar)"
        java -jar "$jar_path"
    fi
}

clean_all() {
    echo -e "${YELLOW}Nettoyage du projet...${NC}"
    "$GRADLE" clean
    rm -f data/*.mv.db data/*.trace.db data/*.db 2>/dev/null || true
    echo -e "${GREEN}[OK] Projet nettoye${NC}"
    echo ""
}

build_all() {
    local jar_path

    echo -e "${YELLOW}[1/3] Compilation du projet...${NC}"
    "$GRADLE" clean classes -x test
    echo -e "${YELLOW}[2/3] Execution des tests...${NC}"
    "$GRADLE" test
    echo -e "${YELLOW}[3/3] Creation du JAR executable...${NC}"
    "$GRADLE" bootJar
    jar_path="$(find_jar)"
    echo -e "${GREEN}[OK] Build complet termine : ${jar_path}${NC}"
    echo ""
}

show_info() {
    echo -e "${GREEN}Application prete.${NC}"
    echo ""
    echo "URLs disponibles :"
    echo "   - API REST : http://localhost:8080"
    echo "   - OpenAPI JSON : http://localhost:8080/api-docs"
    echo "   - H2 Console : http://localhost:8080/h2-console"
    echo ""
    echo "Base H2 :"
    echo "   - URL JDBC : jdbc:h2:file:./data/lieux_db;MODE=MySQL;DB_CLOSE_DELAY=-1"
    echo "   - Username : sa"
    echo "   - Password : (vide)"
    echo ""
}

print_header

case "${1:-run}" in
    compile)
        compile_app
        ;;
    test)
        test_app
        ;;
    package)
        package_app
        show_info
        ;;
    run)
        run_app
        ;;
    clean)
        clean_all
        ;;
    all)
        build_all
        show_info
        ;;
    *)
        echo "Usage: $0 {compile|test|package|run|clean|all}"
        echo ""
        echo "  compile  - Compile le code source"
        echo "  test     - Lance les tests"
        echo "  package  - Cree le JAR executable"
        echo "  run      - Lance l'application (defaut)"
        echo "  clean    - Nettoie le projet"
        echo "  all      - Compile, teste et package"
        exit 1
        ;;
esac
