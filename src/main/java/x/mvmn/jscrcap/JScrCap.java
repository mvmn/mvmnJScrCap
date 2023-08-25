package x.mvmn.jscrcap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;

import x.mvmn.jscrcap.gui.swing.ControlWindow;
import x.mvmn.jscrcap.util.swing.JMenuBarBuilder;
import x.mvmn.jscrcap.util.swing.SwingUtil;

public class JScrCap {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            Stream.of(FlatLightLaf.class, FlatIntelliJLaf.class, FlatDarkLaf.class, FlatDarculaLaf.class)
                    .forEach(lafClass -> UIManager.installLookAndFeel(lafClass.getSimpleName(),
                            lafClass.getCanonicalName()));

            ControlWindow ctrlWin = new ControlWindow();
            JMenuBarBuilder.JMenuBuilder menuBuilder = new JMenuBarBuilder()
                    .menu("Look&Feel");
            String currentLnF = UIManager.getLookAndFeel().getName();
            List<JCheckBoxMenuItem> lnfOptions = new ArrayList<>();
            Arrays.stream(UIManager.getInstalledLookAndFeels())
                    .map(LookAndFeelInfo::getName)
                    .forEach(lnf -> menuBuilder.item(lnf).checkbox().checked(currentLnF.equals(lnf)).actr(e -> {
                        SwingUtil.setLookAndFeel(lnf);
                        lnfOptions.forEach(mi -> mi.setState(lnf.equals(mi.getText())));
                    }).process(mi -> lnfOptions.add((JCheckBoxMenuItem) mi)).build());

            ctrlWin.setJMenuBar(menuBuilder.build().build());
            ctrlWin.setVisible(true);
        });
    }
}
