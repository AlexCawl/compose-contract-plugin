// RUN_PIPELINE_TILL: FRONTEND

package invalidversions

import com.alexcawl.contract.GenerateContract
import com.alexcawl.contract.IntroducedAtContract

@GenerateContract
interface InvalidVersions {
    <!UNSUPPORTED!>@IntroducedAtContract(0)<!>
    fun zero(): Int = 0
    <!UNSUPPORTED!>@IntroducedAtContract(-1)<!>
    fun negative(): Int = 0
    <!UNSUPPORTED!>@IntroducedAtContract(1)<!>
    fun required(): Int
}

<!UNSUPPORTED!>@IntroducedAtContract(1)<!>
fun topLevel(): Int = 0

interface Plain {
    <!UNSUPPORTED!>@IntroducedAtContract(1)<!>
    fun member(): Int = 0
}

class Ordinary {
    <!UNSUPPORTED!>@IntroducedAtContract(1)<!>
    fun member(): Int = 0
}

@GenerateContract
interface LocalMethod {
    fun outer(): Int {
        <!UNSUPPORTED!>@IntroducedAtContract(1)<!>
        fun local(): Int = 0
        return local()
    }
}
