import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import javax.swing.table.AbstractTableModel;

class GornerTableModel extends AbstractTableModel {
    private Double[] coefficients;
    private Double from;
    private Double to;
    private Double step;
    private double result[] = new double[1];

    public GornerTableModel(Double from, Double to, Double step, Double[] coefficients) {
        this.from = from;
        this.to = to;
        this.step = step;
        this.coefficients = coefficients;
    }

    public Double getFrom() {
        return from;
    }

    public Double getTo() {
        return to;
    }

    public Double getStep() {
        return step;
    }

    public int getColumnCount() {
        return 3;
    }

    @Override
    public int getRowCount() {
        // вычислить количество точек между началом и концом отрезка исходя из шага
        // тубулирования
        return new Double(Math.ceil((to - from) / step)).intValue() + 1;
    }

    @Override
    public Object getValueAt(int row, int col) {
        // вычислить Х как начало_отрезка + шаг * номер_строки
        double x = from + step * row;
        switch (col) {
            case 0:
                // если запрашивается значение 1-го столбца, то это Х
                return x;
            case 1:
                // если запрашивается значение 2-го столбца, то это значение многочлена
                result[0] = 0.0;
                for (int i = 0; i < coefficients.length; i++) {
                    result[0] += Math.pow(x, coefficients.length - 1 - i) * coefficients[i];
                }
                return result[0];
            default:
                result[0] = 0.0;
                for (int i = 0; i < coefficients.length; i++) {
                    result[0] += Math.pow(x, coefficients.length - 1 - i) * coefficients[i];
                }
                String[] splitter = String.valueOf(step).split("\\.");
                int a = splitter[1].length();
                int src = (int) result[0];
                double src1 = (result[0] - src) * Math.pow(10.0, a);
                src = (int) src1;
                long tst = (long) (Math.sqrt(src) + 0.5);
                if (tst * tst == src && src != 0) {
                    return true;
                } else {
                    return false;
                }
        }
    }

    @Override
    public String getColumnName(int col) {
        switch (col) {
            case 0:
                return "Значение Х";
            case 1:
                return "Значение многочлена";
            default:
                return "Является ли квадратом";
        }
    }

    @Override
    public Class<?> getColumnClass(int col) {
        // и в 1-ом и во 2-ом столбце находятся значения типа Double в 3 Boolean
        switch (col) {
            case 2:
                return Boolean.class;
            default:
                return Double.class;
        }
    }
}

class GornerTableCellRenderer implements TableCellRenderer {
    private JPanel panel = new JPanel();
    private JLabel label = new JLabel();
    // ищем ячейки, строковое представление совпадает с needle(иголкой)
    private String needle = null;
    private DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance();

    public GornerTableCellRenderer() {
        // Показать только 5 знаков после запятой
        formatter.setMaximumFractionDigits(5);
        formatter.setGroupingUsed(false);
        // установить в качестве разделителя дробной части точку
        DecimalFormatSymbols dottedDouble = formatter.getDecimalFormatSymbols();
        dottedDouble.setDecimalSeparator('.');
        formatter.setDecimalFormatSymbols(dottedDouble);
        // разместить надпись внутри панели
        panel.add(label);
        // установить выравнивание надписи по левому краю
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int col) {
        // преобразовать в Double с поощью форматировщика
        String formattedDouble = formatter.format(value);
        // установить текст надписи равным строковуму представлению числа
        label.setText(formattedDouble);
        if (col == 1 && needle != null && needle.equals(formattedDouble)) {
            // номер столбца 1 значит 2 столбец + иголка не null значит что-то ищем +
            // значение иголки
            // совподает со значением ячейки значит окрашиваем в крассный
            panel.setBackground(Color.RED);
        } else {
            // иначе в белый
            panel.setBackground(Color.WHITE);
        }
        return panel;
    }

    public void setNeedle(String needle) {
        this.needle = needle;
    }
}

class MainFrame extends JFrame {
    // разиеры окна
    private static final int WIDTH = 700;
    private static final int HEIGHT = 700;
    // массив коэффициентов многочлена
    private Double[] coefficients;
    // Объект диалогового окна для выбора файлов
    private JFileChooser fileChooser = null;
    // Элементы меню
    private JMenuItem saveToTextMenuItem;
    private JMenuItem saveToGraphicsMenuItem;
    private JMenuItem searchValueMenuItem;
    private JMenuItem showInfMenuItem;
    // поля ввода для считывания значений переменных
    private JTextField textFieldFrom;
    private JTextField textFieldTo;
    private JTextField textFieldStep;
    private Box hBoxResult;
    // модель данный с результатами вычислений
    private GornerTableModel data;

    MainFrame(Double[] coefficients) {
        // вызов конструктора предка
        super("Тубулирование многочлена на отрезке по схеме Горнера ");
        // передаем во внутреннее поле кэфы
        this.coefficients = coefficients;
        // визуализатор ячеек таблицы
        GornerTableCellRenderer renderer = new GornerTableCellRenderer();
        // размеры окна
        setSize(WIDTH, HEIGHT);
        Toolkit kit = Toolkit.getDefaultToolkit();
        setLocation((kit.getScreenSize().width - WIDTH) / 2, (kit.getScreenSize().height - HEIGHT) / 2);
        // создание меню
        JMenuBar menuBar = new JMenuBar();
        // Установить меню в качестве главного меню приложения
        setJMenuBar(menuBar);
        // Добавить в меню пункт "файл"
        JMenu fileMenu = new JMenu("Файл");
        // добавить его в главное меню
        menuBar.add(fileMenu);
        // создаем пункт "таблица"
        JMenu tableMenu = new JMenu("Таблица");
        menuBar.add(tableMenu);
        // создаем пункт справка
        JMenu referenceMenu = new JMenu("Справка");
        menuBar.add(referenceMenu);
        // Создать новое "действие" по осохранению в текстовый файл
        Action saveToTextAction = new AbstractAction("Сохранить в текстовый файл") {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (fileChooser == null) {
                    // если экземпляр не создан - то создаем его
                    fileChooser = new JFileChooser();
                    // и инициализируем текущей директивой
                    fileChooser.setCurrentDirectory(new File("."));
                }
                // показать диалоговое окно
                if (fileChooser.showSaveDialog(MainFrame.this) == JFileChooser.APPROVE_OPTION) {
                    // если результат его показа успешны сохраняем данные в файл
                    saveToTextFile(fileChooser.getSelectedFile());
                }
            }
        };
        // добавить соответсвующий пункт подменю в меню "Файл"
        saveToTextMenuItem = fileMenu.add(saveToTextAction);
        saveToTextMenuItem.setEnabled(false);
        // Создать новое "действие" по осохранению в текстовый файл
        Action saveToGraphicsAction = new AbstractAction("Сохранить данные для построения графика") {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (fileChooser == null) {
                    // если экземпляр диалогового окна еще не создан - то создаем его
                    fileChooser = new JFileChooser();
                    // и инициализируем текущей директивой
                    fileChooser.setCurrentDirectory(new File("."));
                }
                if (fileChooser.showSaveDialog(MainFrame.this) == JFileChooser.APPROVE_OPTION) {
                    // если результат его показа успешны сохраняем данные в двоичный файл
                    saveToGraphicsFile(fileChooser.getSelectedFile());
                }
            }
        };
        // добавить соответсвующий пункт подменю в меню "Файл"
        saveToGraphicsMenuItem = fileMenu.add(saveToGraphicsAction);
        saveToGraphicsMenuItem.setEnabled(false);
        // Создать новое "действие" по поиску значений многчлена
        Action searchValueAction = new AbstractAction("Найти значение многочлена") {
            @Override
            public void actionPerformed(ActionEvent event) {
                // запросить пользователя ввести искомую строчку
                String value = JOptionPane.showInputDialog(MainFrame.this, "Введите значение для поиска",
                        "Поиск значения", JOptionPane.QUESTION_MESSAGE);
                //
                renderer.setNeedle(value);
                // обновить таблицуустановить значение в качестве иголки
                getContentPane().repaint();
            }
        };
        // добавить действие в меню "Таблица"
        searchValueMenuItem = tableMenu.add(searchValueAction);
        searchValueMenuItem.setEnabled(false);
        Action refereceOboutMe = new AbstractAction("об авторе") {
            @Override
            public void actionPerformed(ActionEvent event) {
                JOptionPane.showMessageDialog(MainFrame.this, "Кононович, 8 группа", "Справка",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        };
        // добавить действие в подменю справка
        showInfMenuItem = referenceMenu.add(refereceOboutMe);
        showInfMenuItem.setEnabled(true);
        // создаем области с полями для границ ввода на отрезке с шагом
        JLabel labelForFrom = new JLabel("X изменяется на интервале от:");
        textFieldFrom = new JTextField("0.0", 10);
        textFieldFrom.setMaximumSize(textFieldFrom.getPreferredSize());
        // поле до границы
        JLabel labelForTo = new JLabel("до");
        textFieldTo = new JTextField("1.0", 10);
        textFieldTo.setMaximumSize(textFieldTo.getPreferredSize());
        // поле с шагом
        JLabel labelForStep = new JLabel("с шагом");
        textFieldStep = new JTextField("0.1", 10);
        textFieldStep.setMaximumSize(textFieldStep.getPreferredSize());
        // создаем контейнер для всего этого
        Box hBoxRange = Box.createHorizontalBox();
        // делаем объемную рамку
        hBoxRange.setBorder(BorderFactory.createBevelBorder(1));
        hBoxRange.add(Box.createHorizontalGlue());
        hBoxRange.add(labelForFrom);
        hBoxRange.add(Box.createVerticalStrut(10));
        hBoxRange.add(textFieldFrom);
        hBoxRange.add(Box.createVerticalStrut(20));
        hBoxRange.add(labelForTo);
        hBoxRange.add(Box.createVerticalStrut(10));
        hBoxRange.add(textFieldTo);
        hBoxRange.add(Box.createVerticalStrut(20));
        hBoxRange.add(labelForStep);
        hBoxRange.add(Box.createVerticalStrut(10));
        hBoxRange.add(textFieldStep);
        hBoxRange.add(Box.createHorizontalGlue());
        // установим размер области вавным удвоенному мин.,что бы при компановке область
        // не сдавили
        hBoxRange.setPreferredSize(new Dimension(new Double(hBoxRange.getMaximumSize().getWidth()).intValue(),
                new Double(hBoxRange.getMaximumSize().getHeight()).intValue() * 2));
        // ставим область в верхнюю часть
        getContentPane().add(hBoxRange, BorderLayout.NORTH);

        // Создаем кнопку вычислить
        JButton buttonCalc = new JButton("Вычислить");
        // задаем действие на нажатие на кнопку
        buttonCalc.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                try {
                    // считать значения начала и конца отрезка, шага
                    Double from = Double.parseDouble(textFieldFrom.getText());
                    Double to = Double.parseDouble(textFieldTo.getText());
                    Double step = Double.parseDouble(textFieldStep.getText());
                    // создаем новый экземпляр модели таблицы на основе считывания данных
                    data = new GornerTableModel(from, to, step, MainFrame.this.coefficients);
                    // новый экземпляр таблицы создаем
                    JTable table = new JTable(data);
                    // установить в качестве визуализатора ячеек для класса Double разработанный
                    // визуализатор
                    table.setDefaultRenderer(Double.class, renderer);
                    // установить размер строки в 30
                    table.setRowHeight(30);
                    // удалить все вложенные элементы из контейнера hBoxResult
                    hBoxResult.removeAll();
                    // Добавить в hBoxResult таблицу с полосами прокрутки
                    hBoxResult.add(new JScrollPane(table));
                    // обновить область содержания главного меню
                    getContentPane().validate();
                    // Пометить ряд элементов меню как доступные
                    saveToTextMenuItem.setEnabled(true);
                    saveToGraphicsMenuItem.setEnabled(true);
                    searchValueMenuItem.setEnabled(true);
                } catch (NumberFormatException ex) {
                    // В случае ошибки преобразования чисел показать сообщение об ошибке
                    JOptionPane.showMessageDialog(MainFrame.this, "Ошибка в формате записи числа с плавающей точкой",
                            "Ошибочный формат числа", JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        // создать кнопку очистить поля
        JButton buttonClear = new JButton("Очистить поля");
        buttonClear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                textFieldFrom.setText("0.0");
                textFieldTo.setText("1.0");
                textFieldStep.setText("0.1");
                // удаляем все вложенные элементы контейнера
                hBoxResult.removeAll();
                // помещаем в контейнер пустую панель
                hBoxResult.add(new JPanel());
                // помечаем элементы меню недоступными
                saveToTextMenuItem.setEnabled(false);
                saveToGraphicsMenuItem.setEnabled(false);
                searchValueMenuItem.setEnabled(false);
                // обновить область содержания главного меню
                getContentPane().validate();
            }
        });
        // Помещаем все кнопки в контейнер
        Box hBoxButtons = Box.createHorizontalBox();
        hBoxButtons.setBorder(BorderFactory.createBevelBorder(1));
        hBoxButtons.add(Box.createHorizontalGlue());
        hBoxButtons.add(buttonCalc);
        hBoxButtons.add(Box.createVerticalStrut(30));
        hBoxButtons.add(buttonClear);
        hBoxButtons.add(Box.createHorizontalGlue());
        // установим размер области вавным удвоенному мин.,что бы при компановке область
        // не сдавили
        hBoxButtons.setPreferredSize(new Dimension(new Double(hBoxButtons.getMaximumSize().getWidth()).intValue(),
                new Double(hBoxButtons.getMaximumSize().getHeight()).intValue() * 2));
        // ставим область в верхнюю часть
        getContentPane().add(hBoxButtons, BorderLayout.SOUTH);
        // область для вывода результата пока что пустая
        hBoxResult = Box.createHorizontalBox();
        hBoxResult.add(new JPanel());
        getContentPane().add(hBoxResult, BorderLayout.CENTER);
    }

    protected void saveToGraphicsFile(File selectedFile) {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(selectedFile));) {
            for (int i = 0; i < data.getRowCount(); i++) {
                out.writeDouble((Double) data.getValueAt(i, 0));
                out.writeDouble((Double) data.getValueAt(i, 1));
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void saveToTextFile(File selectedFile) {
        try {
            // создать новый символьный поток вывода, направленный в указанный файл
            PrintStream out = new PrintStream(selectedFile);
            // записать в поток вывода заглавочное сведение
            out.println("Результаты тубулирования многчлена по схеме Горнера");
            out.println("Многочлен");
            for (int i = 0; i < coefficients.length; i++) {
                out.print(coefficients[i] + "*X^" + (coefficients.length - i - 1));
                if (i != coefficients.length - 1) {
                    out.print(" + ");
                }
            }
            out.println("");
            out.println("Интервал от " + data.getFrom() + " до " + data.getTo() + "с шагом " + data.getStep());
            out.println("====================================================");
            // записать в поток вывода значения в точках
            for (int i = 0; i < data.getRowCount(); i++) {
                out.println("Значение в точке " + data.getValueAt(i, 0) + " равно " + data.getValueAt(i, 1));
            }
            // закрыть поток
            out.close();
        } catch (FileNotFoundException e) {
            // можно не обрабатывать так как мы файл создаем, а не открываем
        }
    }
}

class gorner {
    public static void main(String[] args) {
        // Если не задано и одного аргумента
        // продолжать невозможно
        if (args.length == 0) {
            System.out.println("Невозможно тубулировать многочлен, для которого не задано ни одного коэфициента");
            System.exit(-1);
        }
        // зарезервировать местав массиве столько, сколько аргументов командной строки
        Double[] coefficients = new Double[args.length];
        int i = 0;
        try {
            // перебираем аргументы и пытаемся преобразовать в Double
            for (String arg : args) {
                coefficients[i++] = Double.parseDouble(arg);
            }
        } catch (NumberFormatException ex) {
            // если преобразование не возможно сообщаем об ошибке
            System.out.println("Ошибка преобразования строки '" + args[i] + "' в число типа Double");
            System.exit(-2);
        }
    }
}