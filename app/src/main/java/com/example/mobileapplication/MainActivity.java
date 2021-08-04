package com.example.mobileapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
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
import android.widget.CheckBox;
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

import static android.graphics.Color.RED;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    //пункты меню
    private static final int CM_HIDE_ON_ID = 1;
    private static final int CM_HIDE_OFF_ID = 2;
    //адрес ресурса
    private static final String url = "https://ws-tszh-1c-test.vdgb-soft.ru/api/mobile/news/list";
    private static int item_visible = 0;    //переменная для отображения списка (скрытые / не скрытые новости)
    public TextView tvStatus;
    public DataBaseHelper dataBaseHelper;
    public SimpleCursorAdapter scAdapter;
    public EditText edtFind;
    public ListView lvNews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = (TextView) findViewById(R.id.result_status);
        tvStatus.setText("Новости");

        lvNews = (ListView) findViewById(R.id.lvNews);
        edtFind = (EditText) findViewById(R.id.edtFind);
        Button btnCancel = (Button) findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(this);

        //создаём подключение с БД
        dataBaseHelper = new DataBaseHelper(getApplicationContext());
        dataBaseHelper.open();
        //функция для отображения данных в списке из БД
        listViewShow();
        //если база данных пустая, то выполняем запрос к ресурсу и наполняем её
        if (dataBaseHelper.countData(0) == 0) new getUrlDate().execute(url);

        //по нажатию на строке списка, заменяем фрагмент на фрагмент с Web содержимым
        lvNews.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String mobile_url = dataBaseHelper.getMobileUrl(id);
                Fragment webFragment = new WebFragment();
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                webFragment = WebFragment.getNewInstance(mobile_url);
                ft.replace(R.id.fr_main, webFragment);
                ft.addToBackStack(null);
                ft.commit();
            }
        });
    }

    //функция для отображения данных в списке из БД
    public void listViewShow(){
        //получаем все записи из БД
        Cursor cursor = dataBaseHelper.getData(item_visible);
        //массив с данными из БД
        String[] from  = new String[] {DataBaseHelper.col_title, DataBaseHelper.col_annotation};
        //массив для отображения данных в списке
        int[] to = new int[] {R.id.tvTitle, R.id.tvAnnotation};
        //создание адаптера
        scAdapter = new SimpleCursorAdapter(this, R.layout.news_item, cursor, from, to, 0);
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

                    return dataBaseHelper.getData(item_visible);
                }
                else {
                    return dataBaseHelper.getDataFind(constraint,item_visible);
                }
            }
        });
        //добавляем адаптер к списку
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
            //по пункту меню - обновить, обновляем данные из ресурса
            case R.id.action_update:
                new getUrlDate().execute(url);
                break;
            //отображения скрытых новостей
            case R.id.action_visibleListItem:
                if (item.isChecked()){
                    item_visible=0;
                    item.setChecked(false);
                    listViewShow();
                    Toast.makeText(this, "Отображены новости без скрытых", Toast.LENGTH_SHORT).show();
                }else{
                    if (dataBaseHelper.countData(1)>0){
                        item.setChecked(true);
                        item_visible=1;
                        listViewShow();
                        Toast.makeText(this, "Отображены скрытые новости", Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(this, "Скрытые новости отсутсвуют", Toast.LENGTH_SHORT).show();
                    }
                }
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
            tvStatus.setText("Обновление...");
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
                    for (int i=0; i < jsonArrayNews.length(); i++)
                    {
                        try {
                            JSONObject jsonObjectNews = new JSONObject(jsonArrayNews.getString(i));
                            Integer id = jsonObjectNews.getInt("id");
                            String title = jsonObjectNews.getString("title");
                            String img = jsonObjectNews.getString("img");
                            String news_date = jsonObjectNews.getString("news_date");
                            String news_date_uts = jsonObjectNews.getString("news_date_uts");
                            String annotation = jsonObjectNews.getString("annotation");
                            String mobile_url = jsonObjectNews.getString("mobile_url");
                            dataBaseHelper.AddOrUpdData(id, title, img, news_date, news_date_uts, annotation, mobile_url,0);
                            //обновляем данные в списке
                            listViewShow();
                            tvStatus.setText("Новости");
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
        if (item_visible==0){
            menu.add(0, CM_HIDE_ON_ID, 0, "Скрыть новость");
        }
        else{
            menu.add(0, CM_HIDE_OFF_ID, 0, "Отобразить новость");
        }
    }
    //обработчик выбора для контекстного меню
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == CM_HIDE_ON_ID) {
            // получаем из пункта контекстного меню данные по пункту списка
            AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            // извлекаем id записи и устанвливаем скрытие новости
            dataBaseHelper.visibleItemList(acmi.id,1);
            listViewShow();
            Toast.makeText(this, "Новость скрыта!", Toast.LENGTH_SHORT).show();
            return true;
        }
        if (item.getItemId() == CM_HIDE_OFF_ID) {
            // получаем из пункта контекстного меню данные по пункту списка
            AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            // извлекаем id записи и устанвливаем отображение новости
            dataBaseHelper.visibleItemList(acmi.id,0);
            listViewShow();
            Toast.makeText(this, "Новость отображена!", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dataBaseHelper.close();
    }
}