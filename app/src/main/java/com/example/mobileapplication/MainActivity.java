package com.example.mobileapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor>{

    private static final int CM_HIDE_ID = 1;

    TextView result_status;
    public static final String url = "https://ws-tszh-1c-test.vdgb-soft.ru/api/mobile/news/list";
    DataBaseHelper dataBaseHelper;
    Cursor cursor;
    SimpleCursorAdapter scAdapter;
    ListView lvNews;
    EditText edtFind;
    Fragment fragment;
    Button btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        result_status = (TextView) findViewById(R.id.result_status);
        result_status.setText("Новости");
        edtFind = (EditText) findViewById(R.id.edtFind);
        btnCancel = (Button) findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(this);
        //создаём подключение с БД
        dataBaseHelper = new DataBaseHelper(getApplicationContext());
        dataBaseHelper.open();
        //если база данных пустая, то выполняем запрос к ресурсу и наполняем её
        if (dataBaseHelper.countData() == 0) new getUrlDate().execute(url);

        lvNews = (ListView) findViewById(R.id.lvNews);
        //по нажатию на строке списка, заменяем фрагмент на фрагмент с Web содержимым
        lvNews.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String mobileurl = dataBaseHelper.getMobileUrl(id);
                fragment = new WebFragment();
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                fragment = WebFragment.getNewInstance(mobileurl);
                ft.replace(R.id.fr_main, fragment);
                ft.addToBackStack(null);
                ft.commit();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //получаем все записи из БД
        cursor = dataBaseHelper.getData();
        String[] from;
        from  = new String[] {DataBaseHelper.col_title, DataBaseHelper.col_annotation};
        int[] to = new int[] {R.id.tvTitle, R.id.tvAnnotation};
        scAdapter = new SimpleCursorAdapter(this, R.layout.news_item, cursor, from, to, 0);
        // создаем лоадер для чтения данных
        getSupportLoaderManager().initLoader(0, null, this);
        // если в текстовом поле есть текст, выполняем фильтрацию
        // данная проверка нужна при переходе от одной ориентации экрана к другой
        if(!edtFind.getText().toString().isEmpty())
            scAdapter.getFilter().filter(edtFind.getText().toString());

        // установка слушателя изменения текста
        edtFind.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) { }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            // при изменении текста выполняем фильтрацию
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scAdapter.getFilter().filter(s.toString());
            }
        });

        // устанавливаем провайдер фильтрации
        scAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {

                if (constraint == null || constraint.length() == 0) {

                    return dataBaseHelper.getData();
                }
                else {
                    return dataBaseHelper.getDataFind(constraint);
                }
            }
        });
        lvNews = (ListView) findViewById(R.id.lvNews);
        lvNews.setAdapter(scAdapter);
        // добавляем контекстное меню к списку
        registerForContextMenu(lvNews);
    }

    //создание и обработка пунктов меню
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id){
            case R.id.action_update:
                new getUrlDate().execute(url);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //обработчик нажатия кнопки
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnCancel:
                edtFind.setText("");
                break;
            default:
                break;
        }
    }

    //функция для получения информации из русурса
    private class getUrlDate extends AsyncTask<String, String, String> {

        protected void onPreExecute(){
            super.onPreExecute();
            result_status.setText("Обновление...");
        }

        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(strings[0]);  //открыли urL соединение
                connection = (HttpURLConnection) url.openConnection();  //открыли http соединение
                connection.connect();

                InputStream stream = connection.getInputStream();   //считали полученный поток
                reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));

                StringBuffer buffer = new StringBuffer();
                String line = "";
                while ((line = reader.readLine()) != null){
                    buffer.append(line).append("\n");
                }
                return buffer.toString();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null){
                    connection.disconnect();
                }
                if (reader != null){
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return null;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String result){
            super.onPostExecute(result);

            try {
                JSONObject jsonObject = new JSONObject(result);
                String success;
                success = jsonObject.getString("success");
                if (success.equals("true")){
                    JSONObject jsonObjectData = new JSONObject(jsonObject.getString("data"));
                    JSONArray jsonArrayNews = jsonObjectData.getJSONArray("news");
                    // dataBaseHelper.delData();
                    for (int i=0; i < jsonArrayNews.length(); i++)
                    {
                        try {
                            JSONObject jsonObjectArray1 = new JSONObject(jsonArrayNews.getString(i));

                            Integer id = jsonObjectArray1.getInt("id");
                            String title = jsonObjectArray1.getString("title");
                            String img = jsonObjectArray1.getString("img");
                            String news_date = jsonObjectArray1.getString("news_date");
                            String news_date_uts = jsonObjectArray1.getString("news_date_uts");
                            String annotation = jsonObjectArray1.getString("annotation");
                            String mobile_url = jsonObjectArray1.getString("mobile_url");
                            dataBaseHelper.AddOrUpdData(id, title, img, news_date, news_date_uts, annotation, mobile_url,0);
                            //обновляем лоадер для чтения данных
                            getSupportLoaderManager().getLoader(0).forceLoad();
                            result_status.setText("Новости");
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Ошибка!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    //Отображение в логах сожержимого локальной БД
                   /* cursor = dataBaseHelper.getAllData();
                    if (cursor.moveToFirst()){
                        do{
                            Log.d("myGetJson", "\n" +
                                    " id: " + cursor.getString(0) + "\n" +
                                    " Заголовок: " + cursor.getString(1) + "\n" +
                                    " Адресс картинки: " + cursor.getString(2) + "\n" +
                                    " Дата публикации в ист: " + cursor.getString(3) + "\n" +
                                    " Дата публикации в ист uts: " + cursor.getString(4) + "\n" +
                                    " Анотация: " + cursor.getString(5) + "\n"+
                                    " Новости для моб: " + cursor.getString(6) + "\n"+
                                    " Отображение: " + cursor.getString(7) + "\n");
                        }while (cursor.moveToNext());
                    }*/
                }else{
                    Toast.makeText(MainActivity.this, "Данные с ресурса не получены!", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    //создание контекстного меню для записей из списка
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, CM_HIDE_ID, 0, R.string.view_hide);
    }
    //обработчик выбора для контекстного меню
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == CM_HIDE_ID) {
            // получаем из пункта контекстного меню данные по пункту списка
            AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            // извлекаем id записи и устанвливаем скрытие новости
            dataBaseHelper.visibleItemList(acmi.id);
            getSupportLoaderManager().getLoader(0).forceLoad();
            Toast.makeText(this, "Новость скрыта!", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new MyCursorLoader(this, dataBaseHelper);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        scAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    static class MyCursorLoader extends CursorLoader {
        DataBaseHelper myDbHelper;
        public MyCursorLoader(Context context, DataBaseHelper myDbHelper) {
            super(context);
            this.myDbHelper = myDbHelper;
        }

        @Override
        public Cursor loadInBackground() {
            Cursor cursor=null;
            cursor = myDbHelper.getData();
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return cursor;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dataBaseHelper.close();
    }
}