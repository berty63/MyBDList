# Leçon GitHub — importer et relire le code

1. Dans GitHub, ouvrir `MyBDList` et sélectionner la branche `import-android`.
2. Extraire le ZIP sur l'ordinateur. **Ne pas importer le ZIP comme unique fichier.**
3. Dans GitHub : `Add file` → `Upload files`. Déposer les fichiers et dossiers extraits **à la racine du dépôt** (`app`, `docs`, fichiers Gradle) ; conserver le README existant ou remplacer consciemment son contenu.
4. Vérifier qu'aucun secret, identifiant, APK ou dossier `build/` n'est ajouté.
5. Commit sur `import-android` : `Ajout de la base Android MyBDList V1`.
6. Depuis GitHub : `Compare & pull request` pour voir les différences avant fusion avec `main`.

**Concepts :** la branche est un espace de travail ; le commit enregistre un changement ; la pull request propose la fusion et permet une revue.

**À noter :** GitHub seul ne compile pas l'application. Android Studio est nécessaire pour les premiers essais et pour corriger les erreurs éventuelles.
