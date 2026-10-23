# 📚 MyBDList

Projet personnel d'application Android pour inventorier et gérer une collection de bandes dessinées.

## Objectif

Retrouver facilement ses albums, consulter les séries et tomes possédés, repérer les volumes manquants et enrichir sa collection depuis un téléphone.

## Fonctionnalités prévues (à vérifier au fil du développement)

- Ajout de BD par scan **ISBN/EAN**
- Classement par série, auteur et numéro de tome
- Affichage des couvertures
- Détection des tomes manquants
- Import d'une collection **BDGest**
- Reconnaissance potentielle d'albums à partir d'une photo des tranches
- Utilisation sur plusieurs appareils : synchronisation et authentification à concevoir si retenues

Ces points sont une **feuille de route**, pas une liste de fonctionnalités déjà opérationnelles.

## Technologies envisagées

- **Kotlin** et **Android Studio**
- **Room / SQLite** pour une base locale, sous réserve de confirmation dans le code
- Architecture de synchronisation et service distant : à décider

## Démarrer sur Android Studio

1. Cloner ou ouvrir le dépôt dans Android Studio.
2. Patienter pendant la synchronisation Gradle.
3. Vérifier le SDK Android demandé par le projet et la configuration locale.
4. Sélectionner le module Android effectivement présent et un émulateur ou téléphone.
5. Lancer l'application après une compilation réussie.

Les noms des modules, versions Android et commandes exactes doivent être vérifiés à partir des fichiers Gradle du dépôt ; ce README ne prétend pas qu'une compilation a déjà été validée.

## Feuille de route

- [ ] Valider l'ouverture et la compilation dans Android Studio
- [ ] Décrire la structure existante du code et les écrans
- [ ] Vérifier le stockage local et le schéma des données
- [ ] Implémenter / tester le catalogue et le scan ISBN
- [ ] Définir import/export et sauvegarde des collections
- [ ] Choisir la stratégie multi-appareils et décider si une authentification est nécessaire
- [ ] Ajouter des tests et un guide d'installation reproductible

## Suivi des modifications

Utiliser GitHub Desktop ou Git pour des commits ciblés, par exemple : `Corriger la navigation entre les albums` ou `Ajouter l'écran de collection`.

## Confidentialité

Ne pas publier clés API, mots de passe, jetons d'accès ni collections personnelles contenant des informations sensibles. Utiliser des fichiers de configuration d'exemple et conserver les secrets hors du dépôt.

## Statut

**Développement en cours.** Les étapes précédentes sont à confirmer par des tests de l'application.
