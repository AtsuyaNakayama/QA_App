package jp.techacademy.atsuya.qa_app

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.util.Base64
import android.widget.ListView
import com.google.firebase.database.*

class FavariteList : AppCompatActivity() {
    private var mGenre = 0

    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mListView: ListView
    private lateinit var mFavoriteQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: FavoriteListAdapter

    private var mFavoriteGenreRef: DatabaseReference? = null

    private val mFavoriteEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            mGenre = map["genre"]!!.toInt()
            val databaseReference = FirebaseDatabase.getInstance().reference
            val favoriteRef = databaseReference.child(ContentsPATH).child(map["genre"].toString()).child(dataSnapshot.key.toString())
            favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val map = dataSnapshot.value as Map<String, String>
                    val title = map["title"] ?: ""
                    val body = map["body"] ?: ""
                    val name = map["name"] ?: ""
                    val uid = map["uid"] ?: ""
                    val imageString = map["image"] ?: ""
                    val bytes =
                        if (imageString.isNotEmpty()) {
                            Base64.decode(imageString, Base64.DEFAULT)
                        } else {
                            byteArrayOf()
                        }

                    val answerArrayList = ArrayList<Answer>()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            answerArrayList.add(answer)
                        }
                    }

                    val question = Question(title, body, name, uid, dataSnapshot.key ?: "",
                        mGenre, bytes, answerArrayList)
                    mFavoriteQuestionArrayList.add(question)
                    mAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })

        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>
            val databaseReference = FirebaseDatabase.getInstance().reference
            val favoriteRef = databaseReference.child(ContentsPATH).child(map["genre"].toString())
            favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // 変更があったQuestionを探す
                    for (question in mFavoriteQuestionArrayList) {
                        if (dataSnapshot.key.equals(question.questionUid)) {
                            // このアプリで変更がある可能性があるのは回答(Answer)のみ
                            question.answers.clear()
                            val answerMap = map["answers"] as Map<String, String>?
                            if (answerMap != null) {
                                for (key in answerMap.keys) {
                                    val temp = answerMap[key] as Map<String, String>
                                    val answerBody = temp["body"] ?: ""
                                    val answerName = temp["name"] ?: ""
                                    val answerUid = temp["uid"] ?: ""
                                    val answer = Answer(answerBody, answerName, answerUid, key)
                                    question.answers.add(answer)
                                }
                            }

                            mAdapter.notifyDataSetChanged()
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }

        override fun onChildRemoved(p0: DataSnapshot) {

        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {

        }

        override fun onCancelled(p0: DatabaseError) {

        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser!!
        if (user == null) {
            val intent = Intent(applicationContext, MainActivity::class.java)
            intent.putExtra("genre", 0)
            startActivity(intent)
        }

        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        // ListViewの準備
        mListView = findViewById(R.id.listView)
        mAdapter = FavoriteListAdapter(this)
        mFavoriteQuestionArrayList = ArrayList<Question>()
        mAdapter.notifyDataSetChanged()

        mListView.setOnItemClickListener { parent, view, position, id ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mFavoriteQuestionArrayList[position])
            intent.putExtra("genre", mGenre)
            startActivity(intent)
        }

        mFavoriteQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mFavoriteQuestionArrayList)
        mListView.adapter = mAdapter

        // 選択したジャンルにリスナーを登録する
        if (mFavoriteGenreRef != null) {
            mFavoriteGenreRef!!.removeEventListener(mFavoriteEventListener)
        }

        mFavoriteGenreRef = mDatabaseReference.child(FavoritesPATH).child(user.uid)
        mFavoriteGenreRef!!.addChildEventListener(mFavoriteEventListener)
    }
}