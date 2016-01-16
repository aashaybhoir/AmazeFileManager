package com.amaze.filemanager.utils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amaze.filemanager.R;
import com.amaze.filemanager.activities.MainActivity;
import com.amaze.filemanager.adapters.DrawerAdapter;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.filesystem.Operations;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.fragments.Main;
import com.amaze.filemanager.fragments.TabFragment;
import com.amaze.filemanager.services.DeleteTask;
import com.amaze.filemanager.services.ExtractService;
import com.amaze.filemanager.services.ZipTask;
import com.amaze.filemanager.services.asynctasks.SearchTask;
import com.amaze.filemanager.ui.SmbDialog;
import com.amaze.filemanager.ui.drawer.EntryItem;
import com.stericson.RootTools.RootTools;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Created by root on 11/22/15.
 */
public class MainActivityHelper {
    MainActivity mainActivity;
    Futils utils;
    public MainActivityHelper(MainActivity mainActivity){
        this.mainActivity=mainActivity;
        utils=new Futils();
    }
    public void showFailedOperationDialog(ArrayList<BaseFile> failedOps, boolean move, Context contextc){
        MaterialDialog.Builder mat=new MaterialDialog.Builder(contextc);
        mat.title("Operation Unsuccessful");
        if(mainActivity.theme1==1)mat.theme(Theme.DARK);
        mat.positiveColor(Color.parseColor(mainActivity.fabskin));
        mat.positiveText(R.string.cancel);
        String content="Following files were not "+(move?"moved":"copied")+" successfully";
        int k=1;
        for(BaseFile s:failedOps){
            content=content+ "\n" + (k) + ". " + s.getName();
            k++;
        }
        mat.content(content);
        mat.build().show();
    }
    public final BroadcastReceiver mNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)) {
                    Toast.makeText( mainActivity, "Media Mounted", Toast.LENGTH_SHORT).show();
                    String a = intent.getData().getPath();
                    if (a != null && a.trim().length() != 0 && new File(a).exists() && new File(a).canExecute()) {
                        mainActivity.list.add(new EntryItem(new File(a).getName(), a, ContextCompat
                                .getDrawable(mainActivity, R.drawable.ic_sd_storage_white_56dp)));
                        mainActivity.adapter = new DrawerAdapter( mainActivity, mainActivity. list,  mainActivity, mainActivity. Sp);
                        mainActivity.mDrawerList.setAdapter( mainActivity.adapter);
                    } else {
                        mainActivity.refreshDrawer();
                    }
                } else if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {

                    mainActivity.refreshDrawer();
                }
            }
        }
    };
    void mkdir(final int openMode,final String path,final Main ma){
        final MaterialDialog materialDialog=utils.showNameDialog(mainActivity,new String[]{utils.getString(mainActivity, R.string.entername), "",utils.getString(mainActivity,R.string.newfolder),utils.getString(mainActivity, R.string.create),utils.getString(mainActivity,R.string.cancel),null});
        materialDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String a = materialDialog.getInputEditText().getText().toString();
                mkDir(new HFile(openMode,path + "/" + a),ma);
                materialDialog.dismiss();
            }
        });
        materialDialog.show();
    }
    void mkfile(final int openMode,final String path,final Main ma){
        final MaterialDialog materialDialog=utils.showNameDialog(mainActivity,new String[]{utils.getString(mainActivity, R.string.entername), "",utils.getString(mainActivity,R.string.newfolder),utils.getString(mainActivity, R.string.create),utils.getString(mainActivity,R.string.cancel),null});
        materialDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String a = materialDialog.getInputEditText().getText().toString();
                mkDir(new HFile(openMode,path + "/" + a),ma);
                materialDialog.dismiss();
            }
        });
        materialDialog.show();
    }
    public void add(int pos) {
        final Main ma = (Main) ((TabFragment) mainActivity.getSupportFragmentManager().findFragmentById(R.id.content_frame)).getTab();
        switch (pos) {

            case 0:
                final String path = ma.CURRENT_PATH;
                mkdir(ma.openMode,path,ma);
                break;
            case 1:
                final String path1 = ma.CURRENT_PATH;
                mkfile(ma.openMode,path1,ma);
                break;
            case 2:
                SmbDialog smbDialog=new SmbDialog();
                smbDialog.show(mainActivity.getFragmentManager(),"tab");
                break;
            case 3:
                mainActivity.bindDrive();
                break;
        }
    }

    public String getIntegralNames(String path){
        String newPath="";
        switch (Integer.parseInt(path)){
            case 0:
                newPath=mainActivity.getResources().getString(R.string.images);
                break;
            case 1:
                newPath=mainActivity.getResources().getString(R.string.videos);
                break;
            case 2:
                newPath=mainActivity.getResources().getString(R.string.audio);
                break;
            case 3:
                newPath=mainActivity.getResources().getString(R.string.documents);
                break;
            case 4:
                newPath=mainActivity.getResources().getString(R.string.apks);
                break;
            case 5:
                newPath=mainActivity.getResources().getString(R.string.quick);
                break;
            case 6:
                newPath=mainActivity.getResources().getString(R.string.recent);
                break;
        }
        return newPath;
    }
    public void guideDialogForLEXA(String path) {
        final MaterialDialog.Builder x = new MaterialDialog.Builder(mainActivity);
        if (mainActivity.theme1 == 1) x.theme(Theme.DARK);
        x.title(R.string.needsaccess);
        LayoutInflater layoutInflater = (LayoutInflater) mainActivity.getSystemService(mainActivity.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.lexadrawer, null);
        x.customView(view, true);
        // textView
        TextView textView = (TextView) view.findViewById(R.id.description);
        textView.setText(utils.getString(mainActivity, R.string.needsaccesssummary) + path + utils.getString(mainActivity, R.string.needsaccesssummary1));
        ((ImageView) view.findViewById(R.id.icon)).setImageResource(R.drawable.sd_operate_step);
        x.positiveText(R.string.open);
        x.negativeText(R.string.cancel);
        x.positiveColor(Color.parseColor(mainActivity.fabskin));
        x.negativeColor(Color.parseColor(mainActivity.fabskin));
        x.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog materialDialog) {
                triggerStorageAccessFramework();
            }

            @Override
            public void onNegative(MaterialDialog materialDialog) {
                Toast.makeText(mainActivity, R.string.error, Toast.LENGTH_SHORT).show();
            }
        });
        final MaterialDialog y = x.build();
        y.show();
    }

    private void triggerStorageAccessFramework() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        mainActivity.startActivityForResult(intent, 3);
    }
    public void rename(int mode, String f, String f1, final Activity context, boolean rootmode) {
        final Toast toast=Toast.makeText(context,R.string.renaming,Toast.LENGTH_LONG);
        toast.show();
        Operations.rename(new HFile(mode, f), new HFile(mode, f1), rootmode, context, new Operations.ErrorCallBack() {
            @Override
            public void exists(HFile file) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(toast!=null)toast.cancel();
                        Toast.makeText(mainActivity, (R.string.fileexist), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void launchSAF(HFile file) {

            }

            @Override
            public void launchSAF(final HFile file, final HFile file1) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(toast!=null)toast.cancel();
                        mainActivity.oppathe = file.getPath();
                        mainActivity.oppathe1=file1.getPath();
                        mainActivity.operation = mainActivity.RENAME;
                        guideDialogForLEXA(mainActivity.oppathe1);
                    }});
            }

            @Override
            public void done(HFile hFile,final boolean b) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(toast!=null)toast.cancel();
                        if(b){
                            Intent intent = new Intent("loadlist");
                            mainActivity.sendBroadcast(intent);
                        }
                        else Toast.makeText(context,R.string.operationunsuccesful,Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });
    }

    public int checkFolder(final File folder, Context context) {
        boolean lol= Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP,ext=FileUtil.isOnExtSdCard(folder, context);
        if (lol && ext) {
            if (!folder.exists() || !folder.isDirectory()) {
                return 0;
            }

            // On Android 5, trigger storage access framework.
            if (!FileUtil.isWritableNormalOrSaf(folder, context)) {
                guideDialogForLEXA(folder.getPath());
                return 2;
            }
            return 1;
        } else if (Build.VERSION.SDK_INT == 19 && FileUtil.isOnExtSdCard(folder, context)) {
            // Assume that Kitkat workaround works
            return 1;
        } else if (FileUtil.isWritable(new File(folder, "DummyFile"))) {
            return 1;
        } else {
            return 0;
        }
    }

    public void compressFiles(File file, ArrayList<BaseFile> b) {
        int mode = checkFolder(file.getParentFile(), mainActivity);
        if (mode == 2) {
            mainActivity.oppathe = (file.getPath());
            mainActivity.operation = mainActivity.COMPRESS;
            mainActivity.oparrayList = b;
        } else if (mode == 1) {
            Intent intent2 = new Intent(mainActivity, ZipTask.class);
            intent2.putExtra("name", file.getPath());
            intent2.putExtra("files", b);
            mainActivity.startService(intent2);
        } else Toast.makeText(mainActivity, R.string.not_allowed, Toast.LENGTH_SHORT).show();
    }

    public int contains(String a, ArrayList<String[]> b) {
        int i = 0;
        for (String[] x : b) {
            if (x[1].equals(a)) return i;
            i++;

        }
        return -1;
    }
    public void createSmbDialog(final String path, final boolean edit, final Main ma1) {
        final MaterialDialog.Builder ba3 = new MaterialDialog.Builder(mainActivity);
        ba3.title((R.string.smb_con));
        final View v2 = mainActivity.getLayoutInflater().inflate(R.layout.smb_dialog, null);
        final EditText ip = (EditText) v2.findViewById(R.id.editText);
        int color = Color.parseColor(mainActivity.fabskin);
        utils.setTint(ip, color);
        final EditText user = (EditText) v2.findViewById(R.id.editText3);
        utils.setTint(user, color);
        final EditText pass = (EditText) v2.findViewById(R.id.editText2);
        utils.setTint(pass, color);
        final CheckBox ch = (CheckBox) v2.findViewById(R.id.checkBox2);
        utils.setTint(ch, color);
        TextView help = (TextView) v2.findViewById(R.id.wanthelp);
        help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                utils.showSMBHelpDialog(mainActivity);
            }
        });
        ch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ch.isChecked()) {
                    user.setEnabled(false);
                    pass.setEnabled(false);
                } else {
                    user.setEnabled(true);
                    pass.setEnabled(true);

                }
            }
        });
        if (edit) {
            String userp = "", passp = "", ipp = "";
            try {
                jcifs.Config.registerSmbURLHandler();
                URL a = new URL(path);
                String userinfo = a.getUserInfo();
                if (userinfo != null) {
                    String inf = URLDecoder.decode(userinfo, "UTF-8");
                    userp = inf.substring(0, inf.indexOf(":"));
                    passp = inf.substring(inf.indexOf(":") + 1, inf.length());
                    user.setText(userp);
                    pass.setText(passp);
                } else ch.setChecked(true);
                ipp = a.getHost();
                ip.setText(ipp);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

        }else if(path!=null && path.length()>0){
            ip.setText(path);
            user.requestFocus();
        }
        ba3.customView(v2, true);
        if (mainActivity.theme1 == 1) ba3.theme(Theme.DARK);
        ba3.neutralText(R.string.cancel);
        ba3.positiveText(R.string.create);
        if (edit) ba3.negativeText(R.string.delete);
        ba3.positiveColor(color).negativeColor(color).neutralColor(color);
        ba3.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog materialDialog) {
                Main ma = ma1;
                String s[];
                if (ma == null) ma = ((Main)mainActivity. getFragment().getTab());
                String ipa = ip.getText().toString();
                SmbFile smbFile;
                if (ch.isChecked())
                    smbFile = ma.connectingWithSmbServer(new String[]{ipa, "", ""}, true);
                else {
                    String useru = user.getText().toString();
                    String passp = pass.getText().toString();
                    smbFile = ma.connectingWithSmbServer(new String[]{ipa, useru, passp}, false);
                }
                if (smbFile == null) return;
                s = new String[]{parseSmbPath(smbFile.getPath()), smbFile.getPath()};
                try {
                    if (!edit) {
                        ma.loadlist(smbFile.getPath(), false, -1);
                        if (mainActivity.Servers == null) mainActivity.Servers = new ArrayList<>();
                        mainActivity.Servers.add(s);
                        mainActivity.refreshDrawer();
                        mainActivity.grid.addPath(s[0], s[1], mainActivity.SMB, 1);
                    } else {
                        if (mainActivity.Servers == null){
                            mainActivity.Servers = new ArrayList<>();
                        }
                        int i=-1;
                        if ((i=contains(path, mainActivity.Servers)) != -1) {
                            mainActivity.Servers.remove(i);
                            mainActivity.grid.removePath(path, mainActivity.SMB);
                        }
                        mainActivity.Servers.add(s);
                        Collections.sort(mainActivity.Servers, new BookSorter());
                        mainActivity.refreshDrawer();
                        mainActivity.grid.addPath(s[0], s[1], mainActivity.SMB, 1);
                    }
                } catch (Exception e) {
                    Toast.makeText(mainActivity, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }


            @Override
            public void onNegative(MaterialDialog materialDialog) {
                if (mainActivity.Servers.contains(path)) {
                    mainActivity.Servers.remove(path);
                    mainActivity.refreshDrawer();
                    mainActivity.grid.removePath(path, mainActivity.SMB);

                }
            }
        });
        ba3.build().show();

    }

    public void mkFile(final HFile path,final Main ma) {
        final Toast toast=Toast.makeText(ma.getActivity(),R.string.creatingfile,Toast.LENGTH_LONG);
        toast.show();
        Operations.mkfile(path, ma.getActivity(), ma.ROOT_MODE, new Operations.ErrorCallBack() {
            @Override
            public void exists(final HFile file) {
                ma.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(toast!=null)toast.cancel();
                        Toast.makeText(mainActivity, (R.string.fileexist), Toast.LENGTH_SHORT).show();
                        if(ma!=null && ma.getActivity()!=null)
                            mkfile(file.getMode(),file.getPath(),ma);

                    }
                });
            }

            @Override
            public void launchSAF(HFile file) {

                ma.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(toast!=null)toast.cancel();
                        mainActivity.oppathe = path.getPath();
                        mainActivity.operation = mainActivity.NEW_FOLDER;
                        guideDialogForLEXA(mainActivity.oppathe);
                    }});

            }

            @Override
            public void launchSAF(HFile file, HFile file1) {

            }

            @Override
            public void done(HFile hFile,final boolean b) {
                ma.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(toast!=null)toast.cancel();
                        if(b){
                            ma.updateList();
                        }
                        else Toast.makeText(ma.getActivity(),"Operation Failed",Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });
    }
    public void mkDir(final HFile path,final Main ma) {
        final Toast toast=Toast.makeText(ma.getActivity(),R.string.creatingfolder,Toast.LENGTH_LONG);
        toast.show();
        Operations.mkdir(path, ma.getActivity(), ma.ROOT_MODE, new Operations.ErrorCallBack() {
            @Override
            public void exists(final HFile file) {
                ma.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(toast!=null)toast.cancel();
                        Toast.makeText(mainActivity, (R.string.fileexist), Toast.LENGTH_SHORT).show();
                        if(ma!=null && ma.getActivity()!=null)
                        mkdir(file.getMode(),file.getPath(),ma);
                    }
                });
            }

            @Override
            public void launchSAF(HFile file) {
                if(toast!=null)toast.cancel();
                ma.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                mainActivity.oppathe = path.getPath();
                mainActivity.operation = mainActivity.NEW_FOLDER;
                guideDialogForLEXA(mainActivity.oppathe);
            }});

            }

            @Override
            public void launchSAF(HFile file, HFile file1) {

            }

            @Override
           public void done(HFile hFile,final boolean b) {
            ma.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if(toast!=null)toast.cancel();
                    if(b){
                        ma.updateList();
                    }
                    else Toast.makeText(ma.getActivity(),R.string.operationunsuccesful,Toast.LENGTH_SHORT).show();
                }
            });
           }
       });
    }

    public void deleteFiles(ArrayList<BaseFile> files) {
        if (files == null) return;
        if (files.get(0).isSmb()) {
            new DeleteTask(null, mainActivity).execute((files));
            return;
        }
        int mode = checkFolder(new File(files.get(0).getPath()).getParentFile(), mainActivity);
        if (mode == 2) {
            mainActivity.oparrayList = (files);
            mainActivity.operation = mainActivity.DELETE;
        } else if (mode == 1 || mode == 0)
            new DeleteTask(null, mainActivity).execute((files));
    }

    public void extractFile(File file) {
        int mode = checkFolder(file.getParentFile(), mainActivity);
        if (mode == 2) {
            mainActivity.oppathe = (file.getPath());
            mainActivity.operation = mainActivity.EXTRACT;
        } else if (mode == 1) {
            Intent intent = new Intent(mainActivity, ExtractService.class);
            intent.putExtra("zip", file.getPath());
            mainActivity.startService(intent);
        } else Toast.makeText(mainActivity, R.string.not_allowed, Toast.LENGTH_SHORT).show();
    }
    public String parseSmbPath(String a) {
        if (a.contains("@"))
            return "smb://" + a.substring(a.indexOf("@") + 1, a.length());
        else return a;
    }
    public void search() {
        final Main ma = (Main) ((TabFragment) mainActivity.getSupportFragmentManager().findFragmentById(R.id.content_frame)).getTab();
        final String fpath = ma.CURRENT_PATH;
        final MaterialDialog.Builder a = new MaterialDialog.Builder( mainActivity);
        a.title(R.string.search);
        a.input(utils.getString(mainActivity, R.string.enterfile), "", true, new MaterialDialog
                .InputCallback() {
            @Override
            public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {
            }
        });
        if ( mainActivity.theme1 == 1) a.theme(Theme.DARK);
        a.negativeText(R.string.cancel);
        a.positiveText(R.string.search);
        a.widgetColor(Color.parseColor(mainActivity.fabskin));
        a.positiveColor(Color.parseColor(mainActivity.fabskin));
        a.negativeColor(Color.parseColor(mainActivity.fabskin));
        a.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                materialDialog.dismiss();
                String a = materialDialog.getInputEditText().getText().toString();
                if (a.length() == 0) {
                    return;
                }
                SearchTask task = new SearchTask(ma.searchHelper, ma, a);
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, fpath);
                ma.searchTask = task;

            }
        });
        MaterialDialog b = a.build();
        if (ma.openMode==2) b.getActionButton(DialogAction.POSITIVE).setEnabled(false);
        b.show();
    }

}
