#include <windows.h>
#include <strsafe.h>
#include <stdio.h>
#include "com_epam_deltix_util_jni_WindowsService.h"


static char						mServiceName [1024];
static SERVICE_STATUS			mServiceStatus; 
static SERVICE_STATUS_HANDLE	mServiceStatusHandle;
static JavaVM *					mVM;
static jobject					mInstance;
static jmethodID				mControlMethodId;

static void		tdbg (const char *msg) {
	DWORD		err = GetLastError();
   DWORD dwRet;
   LPTSTR lpszTemp = NULL;

   dwRet = FormatMessageA( FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM |FORMAT_MESSAGE_ARGUMENT_ARRAY,
                          NULL,
                          err,
                          LANG_NEUTRAL,
                          (LPTSTR)&lpszTemp,
                          0,
                          NULL );
/*
	FILE	*TMP = fopen ("D:\\Temp\\Service\\temp.txt", "w");
	fprintf (TMP, "%d: %s: %s", err, msg, lpszTemp);
	fclose (TMP);
*/
	if ( lpszTemp )
      LocalFree((HLOCAL) lpszTemp );
}

VOID SvcDebugOut(LPSTR String, DWORD Status) 
{ 
   CHAR  Buffer[1024]; 
   if (strlen(String) < 1000) 
   { 
      StringCchPrintfA(Buffer, 1024, String, Status); 
      OutputDebugStringA(Buffer); 
   } 
}

VOID WINAPI serviceCtrlHandler (DWORD command) { 
	JNIEnv *			env;

	mVM -> AttachCurrentThread ((void**) &env, NULL);
	env -> CallVoidMethod (mInstance, mControlMethodId, (jint) command);
}

static void			updateStatus () {
	if (!SetServiceStatus (mServiceStatusHandle, &mServiceStatus)) { 
        tdbg ("SetServiceStatus failed");
    } 
}

void WINAPI			serviceStart (DWORD argc, LPTSTR *argv) { 
	JNIEnv *			env;

	mVM -> AttachCurrentThread ((void**) &env, NULL);

	tdbg ("serviceStart");

	mControlMethodId =
		env -> GetMethodID (
			env -> GetObjectClass (mInstance),
			"control",
			"(I)V"
		);

    mServiceStatusHandle = 
		RegisterServiceCtrlHandlerA ( 
			mServiceName, 
			serviceCtrlHandler
		); 
 
    if (mServiceStatusHandle == (SERVICE_STATUS_HANDLE) 0) { 
        tdbg ("RegisterServiceCtrlHandler failed"); 
        return; 
    } 


	//	We update the first time from C++, then Java takes over.
    mServiceStatus.dwServiceType        = SERVICE_WIN32; 
    mServiceStatus.dwCurrentState       = SERVICE_START_PENDING; 
    mServiceStatus.dwControlsAccepted   = 0; 
    mServiceStatus.dwWin32ExitCode      = 0; 
    mServiceStatus.dwServiceSpecificExitCode = 0; 
    mServiceStatus.dwCheckPoint         = 0; 
    mServiceStatus.dwWaitHint           = 10000; 
 
    updateStatus ();
 
	jmethodID	runServiceMethodId =
		env -> GetMethodID (
			env -> GetObjectClass (mInstance),
			"runService",
			"()V"
		);

	env -> CallVoidMethod (mInstance, runServiceMethodId);
} 


JNIEXPORT jstring JNICALL Java_com_epam_deltix_util_jni_WindowsService_getExecutablePath
  (JNIEnv *env, jclass cls)
{
	char		cpath [2048];

	if (GetModuleFileNameA (NULL, cpath, sizeof (cpath)) == 0)
		return (NULL);

	return env -> NewStringUTF (cpath);
}

JNIEXPORT void JNICALL Java_com_epam_deltix_util_jni_WindowsService_setWaitHint (
	JNIEnv *			env, 
	jobject				obj, 
	jint				millis
)
{
	mServiceStatus.dwWaitHint = millis; 
}

JNIEXPORT void JNICALL Java_com_epam_deltix_util_jni_WindowsService_setErrorCode (
	JNIEnv *			env, 
	jobject				obj, 
	jint				error
)
{
	mServiceStatus.dwWin32ExitCode = error;
}

JNIEXPORT void JNICALL Java_com_epam_deltix_util_jni_WindowsService_setAcceptCtrlMask (
	JNIEnv *			env, 
	jobject				obj, 
	jint				mask
)
{
	mServiceStatus.dwControlsAccepted = mask;
}

JNIEXPORT void JNICALL Java_com_epam_deltix_util_jni_WindowsService_setStatus (
	JNIEnv *			env, 
	jobject				obj, 
	jint				status
)
{
	if (status == mServiceStatus.dwCurrentState)
		mServiceStatus.dwCheckPoint++;
	else
		mServiceStatus.dwCheckPoint = 0;

	mServiceStatus.dwCurrentState = status; 
}

JNIEXPORT void JNICALL Java_com_epam_deltix_util_jni_WindowsService_run
  (JNIEnv *env, jobject obj, jstring name)
{
	env -> GetJavaVM (&mVM);
	mInstance = obj;

	jboolean			isCopy;
	const char *		cs =
		env -> GetStringUTFChars (name, &isCopy);

#ifdef __GOT_SECURE_LIB__
    strcpy_s (mServiceName, sizeof (mServiceName), cs);
#else
#   pragma warning(disable: 4995) 
    strcpy (mServiceName, cs);
#endif

	if (isCopy)
		env -> ReleaseStringUTFChars (name, cs);

	SERVICE_TABLE_ENTRYA   DispatchTable [] = { 
		{ mServiceName, (LPSERVICE_MAIN_FUNCTIONA) serviceStart }, 
		{ NULL, NULL } 
	}; 
 
	if (!StartServiceCtrlDispatcherA (DispatchTable)) { 
		tdbg ("AFTER StartServiceCtrlDispatcherA WITH ERROR ");
	} 

	mVM = NULL;
	mInstance = NULL;
}

JNIEXPORT void JNICALL Java_com_epam_deltix_util_jni_WindowsService_reportStatus (
	JNIEnv *			env, 
	jobject				obj
)
{
    updateStatus (); 
}

