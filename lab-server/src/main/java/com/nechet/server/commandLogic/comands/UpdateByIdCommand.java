package com.nechet.server.commandLogic.comands;

import com.nechet.common.util.exceptions.CreateObjectException;
import com.nechet.common.util.exceptions.WrongValuesOfCommandArgumentException;
import com.nechet.common.util.model.Creators.BaseObjectUserCreator;
import com.nechet.common.util.model.SpaceMarine;
import com.nechet.common.util.requestLogic.CommandDescription;
import com.nechet.server.system.SpaceMarinesManager;
import com.nechet.server.system.UserConsole;
import com.nechet.server.system.Utils;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;
import java.util.TreeSet;

public class
UpdateByIdCommand implements BaseCommand {
    private final String name = "update";
    private String result = "";
    public String getResult(){
        return result;
    }
    @Override
    public void execute(CommandDescription d){
        SpaceMarinesManager colMan = SpaceMarinesManager.getInstance();
        TreeSet<SpaceMarine> coll = colMan.getCollection();
        ArrayList<String> str = d.getArgs();
        long id;
        try{
            id = Long.parseLong(str.get(1));
            if(!coll.removeIf(marine -> Objects.equals(marine.getId(), id)))
            {
                result+=("Элемента с таким Id коллекции нет");
            }
            else{
                SpaceMarine updateMarine = d.getObjectArray().get(0);
                updateMarine.setId(id);
                coll.add(updateMarine);
                colMan.setCollection(coll);
            }
        } catch (NumberFormatException e) {
            result+=("Неправильный ввод значения Id. Попробуйте заново.");
        }

    }

    @Override
    public String descr() {
        return "Обновляет элемент, Id которого равен указанному.\n" +
                "   Аргумент команды: Id;    Тип: Long";
    }

    @Override
    public String getName() {
        return this.name;
    }
}
