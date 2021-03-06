package beatbox3;

import java.awt.*;
import javax.swing.*;
import java.io.*;
import javax.sound.midi.*;
import java.util.*;
import java.awt.event.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.*;

public class BeatBox3 {

    JFrame theFrame;
    JPanel mainPanel;
    JList incomingList;
    JTextField userMessage;
    ArrayList<JCheckBox> checkBoxList;
    int nextNum;
    Vector<String> listVector = new Vector<String>();
    String userName;
    ObjectOutputStream out;
    ObjectInputStream in;
    HashMap<String, boolean[]> otherSeqsMap = new HashMap<String, boolean[]>();
    Sequencer sequencer;
    Sequence sequence;
    Sequence mySequence = null;
    Track track;

    String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat",
        "Acoustic Snare", "Crash Cymbal", "Hand Clap", "High Tom", "Hi Bingo",
        "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom",
        "High Agogo", "Open Hi Conga"};

    int[] instruments = {35, 42, 46,
        38, 49, 39, 50, 60,
        70, 72, 64, 56, 58, 47,
        67, 63};

    public static void main(String[] args) {

//new BeatBox3().StartUp(args[0]);
        new BeatBox3().buildGui();
    }

    public void StartUp(String name) {
        userName = name;
        try {
            Socket sock = new Socket("127.0.0.1", 4242);
            out = new ObjectOutputStream(sock.getOutputStream());
            in = new ObjectInputStream(sock.getInputStream());

            Thread remote = new Thread(new Runnable() {
                boolean[] checkBoxState = null;
                String nameToShow = null;
                Object obj = null;

                public void run() {
                    try {
                        while ((obj = in.readObject()) != null) {
                            System.out.println("got an object");
                            System.out.println(obj.getClass());
                            nameToShow = (String) obj;
                            checkBoxState = (boolean[]) in.readObject();
                            otherSeqsMap.put(nameToShow, checkBoxState);
                            listVector.add(nameToShow);
                            incomingList.setListData(listVector);
                        }
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                }

            });
            remote.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // setUpMidi();
        buildGui();

    }

    public void buildGui() {
        theFrame = new JFrame("Cyber BeatBox");
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        checkBoxList = new ArrayList<JCheckBox>();

        Box buttonBox = new Box(BoxLayout.Y_AXIS);
        JButton start = new JButton("START");
        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                BuildTrackAndStart();
            }
        });
        buttonBox.add(start);

        JButton stop = new JButton("STOP");
        stop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                sequencer.stop();
            }
        });
        buttonBox.add(stop);

        JButton upTempo = new JButton("TEMPO UP");
        upTempo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                float fl = sequencer.getTempoFactor();
                sequencer.setTempoFactor((float) (fl * 1.03));
            }
        });
        buttonBox.add(upTempo);

        JButton downTempo = new JButton("TEMPO DOWN");
        start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                float ar = sequencer.getTempoFactor();
                sequencer.setTempoFactor((float) (ar * 0.97));
            }
        });
        buttonBox.add(downTempo);

        JButton sendIt = new JButton("SEND");
        start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                boolean[] checkboxState = new boolean[256];
                for (int i = 0; i < 256; i++) {
                    JCheckBox check = (JCheckBox) checkBoxList.get(i);
                    if (check.isSelected()) {
                        checkboxState[i] = true;
                    }
                }
                String messageToSend = null;
                try {
                    out.writeObject(userName + nextNum++ + ": " + userMessage.getText());
                    out.writeObject(checkboxState);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                userMessage.setText("");
            }

        });
        buttonBox.add(sendIt);

        userMessage = new JTextField();
        buttonBox.add(userMessage);

        incomingList = new JList();
        incomingList.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    String selected = (String) incomingList.getSelectedValue();

                    if (selected != null) {
                        boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected);
                        //changeSequence(selectedState);
                        sequencer.stop();
                        BuildTrackAndStart();
                    }
                }

            }

        });
        incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane theList = new JScrollPane(incomingList);
        buttonBox.add(theList);
        incomingList.setListData(listVector);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (int i = 0; i < 16; i++) {
            nameBox.add(new Label(instrumentNames[i]));
        }
        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, nameBox);

        theFrame.getContentPane().add(background);
        GridLayout grid = new GridLayout(16, 16);
        grid.setVgap(1);
        grid.setHgap(2);

        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);

        for (int i = 0; i < 256; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkBoxList.add(c);
            mainPanel.add(c);
        }
        theFrame.setBounds(50, 50, 300, 300);
        theFrame.pack();
        theFrame.setVisible(true);
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainPanel.setBackground(Color.yellow);

    }

    public void setUpMidi() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void BuildTrackAndStart() {
        ArrayList<Integer> trackList;
        sequence.deleteTrack(track);
        track = sequence.createTrack();

        for (int i = 0; i < 16; i++) {
            trackList = new ArrayList<Integer>();
            for (int j = 0; i < 16; i++) {
                JCheckBox jc = (JCheckBox) checkBoxList.get(j + (16 * i));
                if (jc.isSelected()) {
                    int key = instruments[i];
                    trackList.add(new Integer(key));
                } else {
                    trackList.add(null);
                }

            }
//makeTracks(trackList);
        }
        track.add(makeEvent(192, 9, 1, 0, 15));
        try {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            sequencer.setTempoInBPM(120);

        } catch (Exception ee) {
            ee.printStackTrace();
        }
    }

    public MidiEvent makeEvent(int comb, int chan, int one, int two, int tick) {
        MidiEvent event = null;
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(comb, chan, one, two);
            event = new MidiEvent(a, tick);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return event;
    }
}
