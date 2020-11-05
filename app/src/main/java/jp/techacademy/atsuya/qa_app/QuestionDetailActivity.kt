package jp.techacademy.atsuya.qa_app

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*
import java.util.HashMap

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference

    private var mGenre: Int = 0
    private var mFavoriteState: Boolean = false


    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        val dataBaseReference = FirebaseDatabase.getInstance().reference

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras!!.get("question") as Question
        mGenre = extras!!.get("genre") as Int

        title = mQuestion.title



        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        fab.setOnClickListener {
            var user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }

        // ログイン済みのユーザーを取得する
        var user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val favoritesRef = dataBaseReference.child(FavoritesPATH).child(user!!.uid)
            favoritesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (child in dataSnapshot.children) {
                        if (child.key == mQuestion.questionUid) {
                            mFavoriteState = true
                            invalidateOptionsMenu()
                        }
                    }
                }

                override fun onCancelled(p0: DatabaseError) {}
            })
        }

        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString())
            .child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        var user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            menuInflater.inflate(R.menu.menu_detail, menu)
            if (mFavoriteState) {
                menu?.getItem(0)?.setIcon(android.R.drawable.btn_star_big_on)
            } else {
                menu?.getItem(0)?.setIcon(android.R.drawable.btn_star_big_off)
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        var user = FirebaseAuth.getInstance().currentUser
        val dataBaseReference = FirebaseDatabase.getInstance().reference
        if (id == R.id.star) {
            val data = HashMap<String, String>()
            data["genre"] = mGenre.toString()

            if (!mFavoriteState) {
                // お気に入り登録されたときの処理
                item.setIcon(android.R.drawable.btn_star_big_on)
                val favoriteRef = dataBaseReference.child(FavoritesPATH).child(user!!.uid)
                    .child(mQuestion.questionUid)
                Snackbar.make(findViewById(android.R.id.content), "お気に入りに登録されました", Snackbar.LENGTH_LONG).show()
                favoriteRef.setValue(data)

            } else {
                // お気に入り解除されたときの処理
                item.setIcon(android.R.drawable.btn_star_big_off)
                Snackbar.make(findViewById(android.R.id.content), "お気に入りが解除されました", Snackbar.LENGTH_LONG).show()
                val favoriteRef = dataBaseReference.child(FavoritesPATH).child(user!!.uid)
                    .child(mQuestion.questionUid)
                favoriteRef.removeValue()
            }

            mFavoriteState = !mFavoriteState

            return true
        }

        return super.onOptionsItemSelected(item)
    }
}

