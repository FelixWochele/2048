package HighScore;

import DataClasses.GameOptions;
import DataClasses.HighScoreData;

public interface ILocalHighScoreController
{
    /**
     * @param newScore Neue Punktzahl (wird intern geprüft ob es Highscore wird)
     */
    void submitNewScore(int newScore);


    /**
     * Da für die verschiedenen Spielmodi und Feldgrößen die Highscores getrennt gespeichert werden, müssen die Gameoptions dem HighscoreController bekannt sein
     * @param gameOptions Spieloptionen-
     */
    void setGameOptions(GameOptions gameOptions);

    /**
     * @return HighScore-Daten
     */
    HighScoreData getCurrentHighScoreData();
}

